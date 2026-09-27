package com.milind.lazypanel.service.implementations;

import com.milind.lazypanel.constant.AppConstants;
import com.milind.lazypanel.dto.*;
import com.milind.lazypanel.exception.GoogleSheetsException;
import com.milind.lazypanel.exception.ResourceNotFoundException;
import com.milind.lazypanel.model.User;
import com.milind.lazypanel.model.UserSheet;
import com.milind.lazypanel.repository.SheetRepository;
import com.milind.lazypanel.service.interfaces.GoogleSheetsService;
import com.milind.lazypanel.service.interfaces.TokenService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoogleSheetsServiceImpl implements GoogleSheetsService {

    private final RestClient restClient;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private SheetRepository sheetRepository;

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private static final String[] CATEGORIES = {"Essential", "Avoidable", "Fun", "Total"};

    public GoogleSheetsServiceImpl(RestClient.Builder restClient) {
        this.restClient = restClient.baseUrl("https://sheets.googleapis.com/v4/spreadsheets").build();
    }

    @Override
    public SheetStatusResponse getSheetStatus(Long userId) {
        UserSheet sheet = sheetRepository.findByUserId(userId);
        return new SheetStatusResponse(sheet != null);
    }

    @Override
    public SheetsResponseDto appendRowToSheet(Long userId, ArrayList<AddExpenseRequestDto> expenses) {
        log.debug("Appending {} expense(s) to Google Sheet for userId {}", expenses.size(), userId);
        UserSheet sheet = sheetRepository.findByUserId(userId);
        if (sheet == null) {
            log.warn("UserId {} has no configured Google Sheet", userId);
            throw new ResourceNotFoundException("Sheet does not exist");
        }
        Set<String> uniqueMonths = expenses.stream()
                .map(e -> e.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .collect(Collectors.toSet());

        String token = tokenService.getAccessTokenFromUserId(userId);
        //wrap in try/catch?
        SheetsBatchDto.GetResponse batchGetResponse = this.restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/" + sheet.getSpreadsheetId() + "/values:batchGet");
                    for (String month : uniqueMonths) {
                        builder.queryParam("ranges", month + "!A:A");
                    }
                    return builder.build();
                })
                .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                .retrieve()
                .body(SheetsBatchDto.GetResponse.class);

        Map<String, Integer> nextRowMap = new HashMap<>();
        if (batchGetResponse != null && batchGetResponse.getValueRanges() != null) {
            nextRowMap = batchGetResponse.getValueRanges().stream()
                    .collect(Collectors.toMap(
                            valueRange -> valueRange.getRange().split("!")[0],
                            valueRange -> {
                                List<List<String>> values = valueRange.getValues();
                                if (values == null || values.isEmpty()) {
                                    return 1;
                                }
                                return values.size() + 1;
                            }
                    ));
        }

        expenses.sort(Comparator.comparing(AddExpenseRequestDto::getDate));
        Map<String, List<AddExpenseRequestDto>> expensesByMonth = expenses.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                ));

        List<SheetsBatchDto.ValueRange> dataList = new ArrayList<>();

        for (Map.Entry<String, List<AddExpenseRequestDto>> entry : expensesByMonth.entrySet()) {
            String month = entry.getKey();
            List<AddExpenseRequestDto> monthExpenses = entry.getValue();

            int nextRow = nextRowMap.getOrDefault(month, 1);

            List<List<String>> sheetRows = new ArrayList<>();
            for (AddExpenseRequestDto expense : monthExpenses) {
                List<String> row = List.of(
                        expense.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        expense.getDescription(),
                        String.valueOf(expense.getAmount()),
                        expense.getCategory()
                );
                sheetRows.add(row);
            }

            log.debug("Preparing {} expense(s) for month {} starting at row {}", sheetRows.size(), month, nextRow);
            int endRow = nextRow + sheetRows.size() - 1;
            String range = String.format("%s!A%d:D%d", month, nextRow, endRow);
            SheetsBatchDto.ValueRange valueRange = new SheetsBatchDto.ValueRange(range, "ROWS", sheetRows);

            dataList.add(valueRange);
        }

        SheetsBatchDto.UpdateRequest updateRequest = new SheetsBatchDto.UpdateRequest("USER_ENTERED", dataList);
        try {
            SheetsResponseDto response = this.restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/" + sheet.getSpreadsheetId() + "/values:batchUpdate").build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .body(updateRequest)
                    .retrieve()
                    .body(SheetsResponseDto.class);

            log.info("Successfully executed multi-sheet batch update.");

            return response;
        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to append rows to Google Sheet.", e);
        }
    }

    @Override
    @Transactional
    public SheetsResponseDto createAndSetupSheet(User user) {
        //check if sheet already exists
        Long userId = user.getId();
        log.info("Creating Google Sheet for userId {}", userId);
        UserSheet sheet = sheetRepository.findByUserId(userId);

        if (sheet != null) {
            log.warn("Google Sheet already exists for userId {}", userId);
            return new SheetsResponseDto(sheet.getSpreadsheetId());
        }
        String token = tokenService.getAccessTokenFromUserId(userId);
        //as of now the sheet will be like jan - dec [year] but there might be a use case for FY [year] too
        SpreadsheetCreationDto creationPayload = getCreateSpreadsheetPayload();
        try {
            SheetsResponseDto creationResponse = this.restClient.post()
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .body(creationPayload)
                    .retrieve().body(SheetsResponseDto.class);
            String spreadsheetId = creationResponse.getSpreadsheetId();
            log.info("Successfully created Google Sheet {} for userId {}", spreadsheetId, userId);
            SpreadsheetSetupDto setupPayload = getSetupSpreadsheetPayload();
            this.restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/" + spreadsheetId + ":batchUpdate").build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .body(setupPayload)
                    .retrieve().body(SheetsResponseDto.class);
            log.info("Successfully configured Google Sheet for userId {}", userId);
            //save the sheet against the user, can't believe i missed this step
            sheetRepository.save(UserSheet.builder().spreadsheetId(spreadsheetId).user(user).build());
            return new SheetsResponseDto(spreadsheetId);
        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to setup Google Sheet.", e);
        }
    }

    @Override
    public List<Map<String, Double>> getCurrentMonthExpenses(Long userId) {
        UserSheet sheet = sheetRepository.findByUserId(userId);
        if (sheet == null) {
            throw new ResourceNotFoundException("Sheet does not exist");
        }

        String token = tokenService.getAccessTokenFromUserId(userId);
        try {
            SheetRowsDto response = this.restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/" + sheet.getSpreadsheetId() + "/values/B2:M5").queryParam("majorDimension", "COLUMNS").build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .retrieve().body(SheetRowsDto.class);
            if (response.getValues() == null || response.getValues().isEmpty()) {
                log.warn("No expense summary found for current month for userId {}", userId);
            }

            //gonna be list of custom objects now month string and the rest doubles
            List<Map<String, Double>> monthlyCards = new ArrayList<>();

            if (response.getValues() != null && !response.getValues().isEmpty()) {
                for (ArrayList<String> value : response.getValues()) {
                    Map<String, Double> hm = new HashMap<>();
                    for (int i = 0; i < CATEGORIES.length; i++)
                        hm.put(CATEGORIES[i].toLowerCase(), Double.parseDouble(value.get(i)));
                    monthlyCards.add(hm);
                }
            }
            return monthlyCards;
        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to fetch current month expenses", e);
        }
    }

    private SpreadsheetCreationDto getCreateSpreadsheetPayload() {
        SpreadsheetCreationDto sheetCreationDto = SpreadsheetCreationDto.builder()
                .properties(
                        SpreadsheetCreationDto.Properties.builder()
                                .title("2026 Expenditure (LazyPanel)")
                                .build()
                )
                .sheets(new ArrayList<>(List.of(
                        SpreadsheetCreationDto.Sheet.builder()
                                .properties(
                                        SpreadsheetCreationDto.Properties.builder()
                                                .title("Annual Summary")
                                                .sheetId(0)
                                                .index(0)
                                                .build()
                                ).build()
                ))).build();
        for (int i = 0; i < MONTHS.length; i++) {
            sheetCreationDto.sheets.add(
                    SpreadsheetCreationDto.Sheet.builder()
                            .properties(
                                    SpreadsheetCreationDto.Properties.builder()
                                            .title(MONTHS[i])
                                            .sheetId(i + 1)
                                            .index(i + 1)
                                            .build()
                            ).build()
            );
        }

        return sheetCreationDto;
    }

    private SpreadsheetSetupDto getSetupSpreadsheetPayload() {
        SpreadsheetSetupDto spreadsheetSetupDto = SpreadsheetSetupDto.builder().requests(new ArrayList<>()).build();
        addSummarySheetRequests(spreadsheetSetupDto);
        addMonthSheetsRequests(spreadsheetSetupDto);
        return spreadsheetSetupDto;
    }

    private void addMonthSheetsRequests(SpreadsheetSetupDto spreadsheetSetupDto) {
        for (int i = 0; i < MONTHS.length; i++) {
            SpreadsheetSetupDto.Range range = rangeBuilder(i + 1, 0, 1, 0, 4);
            //add the headers
            spreadsheetSetupDto.requests.add(setRowHeadersRequest(
                    range,
                    new String[]{"Date", "Description", "Amount", "Category"}
            ));

            //bold the headers
            spreadsheetSetupDto.requests.add(boldCellsRequest(range));

            //date validation
            spreadsheetSetupDto.requests.add(
                    SpreadsheetSetupDto.Request.builder()
                            .setDataValidation(
                                    SpreadsheetSetupDto.SetDataValidation.builder()
                                            .range(rangeBuilder(i + 1, 1, 0, 1))
                                            .rule(SpreadsheetSetupDto.Rule.builder()
                                                    .condition(SpreadsheetSetupDto.Condition.builder()
                                                            .type("DATE_IS_VALID").build())
                                                    .strict(true).build()).build()
                            ).build()
            );

            //dropdown validation
            SpreadsheetSetupDto.Condition condition = SpreadsheetSetupDto.Condition.builder()
                    .type("ONE_OF_LIST").values(new ArrayList<>()).build();
            for (int j = 0; j < CATEGORIES.length - 1; j++) {
                condition.getValues().add(SpreadsheetSetupDto.Value.builder()
                        .userEnteredValueString(CATEGORIES[j]).build());
            }
            spreadsheetSetupDto.requests.add(
                    SpreadsheetSetupDto.Request.builder()
                            .setDataValidation(
                                    SpreadsheetSetupDto.SetDataValidation.builder()
                                            .range(rangeBuilder(i + 1, 1, 3, 4))
                                            .rule(SpreadsheetSetupDto.Rule.builder()
                                                    .condition(condition)
                                                    .build()).build()
                            ).build()
            );
        }

    }

    private void addSummarySheetRequests(SpreadsheetSetupDto spreadsheetSetupDto) {

        SpreadsheetSetupDto.Range categoryColumnRange =
                rangeBuilder(0, 1, CATEGORIES.length + 1, 0, 1);

        SpreadsheetSetupDto.Range monthRowRange =
                rangeBuilder(0, 0, 1, 1, MONTHS.length + 2);

        //category row header values NOT LIKE THE OTHERS BELOW NEED TO MAKE NAMING CLEAR
        SpreadsheetSetupDto.UpdateCells categoryRowHeaders = SpreadsheetSetupDto.UpdateCells.builder()
                .range(categoryColumnRange)
                .rows(new ArrayList<>()).build();
        for (String category : CATEGORIES) {
            SpreadsheetSetupDto.Value categoryValue = SpreadsheetSetupDto.Value.builder()
                    .userEnteredValue(SpreadsheetSetupDto.UserEnteredValue.builder()
                            .stringValue(category).
                            build()).
                    build();
            categoryRowHeaders.rows.add(SpreadsheetSetupDto.Row.builder().values(new ArrayList<>(
                    List.of(categoryValue)
            )).build());
        }
        //add categories to row headers
        spreadsheetSetupDto.requests.add(SpreadsheetSetupDto.Request.builder().updateCells(categoryRowHeaders).build());

        //category row header bold
        spreadsheetSetupDto.requests.add(boldCellsRequest(categoryColumnRange));
        //month row header bold
        spreadsheetSetupDto.requests.add(boldCellsRequest(monthRowRange));

        //grid cell values
        spreadsheetSetupDto.requests.add(formulaCellsRequest("=IFERROR(SUMIF(INDIRECT(B$1&\"!D:D\"), $A2, INDIRECT(B$1&\"!C:C\")), 0)",
                rangeBuilder(0, 1, 4, 1, MONTHS.length + 1)));
        //total row values
        spreadsheetSetupDto.requests.add(formulaCellsRequest("=SUM(B2:B4)",
                rangeBuilder(0, CATEGORIES.length, CATEGORIES.length + 1, 1, MONTHS.length + 1)));
        //Yearly total column values
        spreadsheetSetupDto.requests.add(formulaCellsRequest("=SUM(B2:M2)",
                rangeBuilder(0, 1, CATEGORIES.length + 1, MONTHS.length + 1, MONTHS.length + 2)));

        //month row headers
        spreadsheetSetupDto.requests.add(setRowHeadersRequest(
                rangeBuilder(0, 0, 1, 1, MONTHS.length + 1), MONTHS
        ));

        //yearly total header
        spreadsheetSetupDto.requests.add(setRowHeadersRequest(
                rangeBuilder(0, 0, 1, MONTHS.length + 1, MONTHS.length + 2), new String[]{"Yearly Total"}
        ));
    }

    private SpreadsheetSetupDto.Request formulaCellsRequest(String formulaValue, SpreadsheetSetupDto.Range range) {
        return SpreadsheetSetupDto.Request.builder().repeatCell(
                SpreadsheetSetupDto.RepeatCell.builder()
                        .range(range)
                        .cell(SpreadsheetSetupDto.Cell.builder()
                                .userEnteredValue(
                                        SpreadsheetSetupDto.UserEnteredValue.builder()
                                                .formulaValue(formulaValue)
                                                .build()
                                ).build())
                        .build()
        ).build();
    }

    private SpreadsheetSetupDto.Request boldCellsRequest(SpreadsheetSetupDto.Range range) {
        return SpreadsheetSetupDto.Request.builder()
                .repeatCell(
                        SpreadsheetSetupDto.RepeatCell.builder()
                                .range(range)
                                .cell(SpreadsheetSetupDto.Cell.builder()
                                        .userEnteredFormat(SpreadsheetSetupDto.UserEnteredFormat.builder()
                                                .textFormat(SpreadsheetSetupDto.TextFormat.builder()
                                                        .build()).build()).build())
                                .fields("userEnteredFormat.textFormat.bold").build()).build();
    }

    private SpreadsheetSetupDto.Range rangeBuilder(Integer sheetId, Integer startRowIndex,
                                                   Integer endRowIndex, Integer startColumnIndex,
                                                   Integer endColumnIndex) {
        return SpreadsheetSetupDto.Range.builder().sheetId(sheetId).startRowIndex(startRowIndex)
                .endRowIndex(endRowIndex).startColumnIndex(startColumnIndex)
                .endColumnIndex(endColumnIndex).build();
    }

    private SpreadsheetSetupDto.Range rangeBuilder(Integer sheetId, Integer startRowIndex,
                                                   Integer startColumnIndex, Integer endColumnIndex) {
        return rangeBuilder(sheetId, startRowIndex, null, startColumnIndex, endColumnIndex);
    }

    private SpreadsheetSetupDto.Request setRowHeadersRequest(SpreadsheetSetupDto.Range range, String[] headers) {
        SpreadsheetSetupDto.Request updateCellsRequest = SpreadsheetSetupDto.Request.builder()
                .updateCells(SpreadsheetSetupDto.UpdateCells.builder()
                        .range(range).rows(new ArrayList<>()).build()).build();

        ArrayList<SpreadsheetSetupDto.Value> values = Arrays.stream(headers).map(
                        header -> SpreadsheetSetupDto.Value.builder().userEnteredValue(
                                SpreadsheetSetupDto.UserEnteredValue.builder()
                                        .stringValue(header)
                                        .build()).build())
                .collect(Collectors.toCollection(ArrayList::new));
        SpreadsheetSetupDto.Row row = SpreadsheetSetupDto.Row.builder()
                .values(values).build();
        updateCellsRequest.updateCells.rows.add(row);
        return updateCellsRequest;
    }
}
