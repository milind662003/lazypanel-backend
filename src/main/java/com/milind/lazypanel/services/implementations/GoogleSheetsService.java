package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.constants.AppConstants;
import com.milind.lazypanel.dto.*;
import com.milind.lazypanel.exception.GoogleSheetsException;
import com.milind.lazypanel.exception.ResourceNotFoundException;
import com.milind.lazypanel.models.User;
import com.milind.lazypanel.models.UserSheet;
import com.milind.lazypanel.repositories.SheetRepository;
import com.milind.lazypanel.services.interfaces.IGoogleSheetsService;
import com.milind.lazypanel.services.interfaces.ITokenService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleSheetsService implements IGoogleSheetsService {

    private final RestClient restClient;

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private SheetRepository sheetRepository;

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private static final String[] CATEGORIES = {"Essential", "Avoidable", "Fun", "Total"};

    public GoogleSheetsService(RestClient.Builder restClient) {
        this.restClient = restClient.baseUrl("https://sheets.googleapis.com/v4/spreadsheets").build();
    }

    @Override
    public SheetStatusResponse getSheetStatus(Long userId) {
        UserSheet sheet = sheetRepository.findByUserId(userId);
        return new SheetStatusResponse(sheet != null);
    }

    //as of now, it is bound to get the month from the current date, not the record details
    //future upgrade: since it currently ignores everything that ain't this month (forced by the UI as well),
    //later i can do a group by month then batchUpdate the sheets so multiple months are an option
    @Override
    public SheetsResponseDto appendRowToSheet(Long userId, ArrayList<AddExpenseRequestDto> expenses) {
        UserSheet sheet = sheetRepository.findByUserId(userId);
        if (sheet == null) {
            throw new ResourceNotFoundException("Sheet does not exist");
        }

        String month = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        SheetRowsDto sheetsRows = new SheetRowsDto(new ArrayList<>());

        for (AddExpenseRequestDto expense : expenses) {

            if (!expense.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).equals(month)) continue;

            ArrayList<String> row = new ArrayList<>();
            String formattedDate = expense.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            row.add(formattedDate);
            row.add(expense.getDescription());
            row.add(String.valueOf(expense.getAmount()));
            row.add(expense.getCategory());
            sheetsRows.getValues().add(row);
        }

        String token = tokenService.getAccessTokenFromUserId(userId);
        try {
            return this.restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/" + sheet.getSpreadsheetId() + "/values/" + month + "!A:D:append")
                            .queryParam("valueInputOption", "USER_ENTERED").build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .body(sheetsRows)
                    .retrieve().body(SheetsResponseDto.class);

        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to append rows to Google Sheet.", e);
        }
    }

    @Override
    @Transactional
    public SheetsResponseDto createAndSetupSheet(User user) {
        //check if sheet already exists
        Long userId = user.getId();
        UserSheet sheet = sheetRepository.findByUserId(userId);

        if (sheet != null) {
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
            SpreadsheetSetupDto setupPayload = getSetupSpreadsheetPayload();
            this.restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/" + spreadsheetId + ":batchUpdate").build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .body(setupPayload)
                    .retrieve().body(SheetsResponseDto.class);
            //save the sheet against the user, can't believe i missed this step
            sheetRepository.save(UserSheet.builder().spreadsheetId(spreadsheetId).user(user).build());
            return new SheetsResponseDto(spreadsheetId);
        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to setup Google Sheet.", e);
        }
    }

    @Override
    public Map<String, Double> getCurrentMonthExpenses(Long userId) {
        UserSheet sheet = sheetRepository.findByUserId(userId);
        if (sheet == null) {
            throw new ResourceNotFoundException("Sheet does not exist");
        }

        int month = LocalDate.now().getMonth().getValue();
        char col = (char) ('A' + month);
        int rowStart = 2;
        int rowEnd = CATEGORIES.length + 1;

        String token = tokenService.getAccessTokenFromUserId(userId);
        try {

            SheetRowsDto response = this.restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/" + sheet.getSpreadsheetId() + "/values/" + col + rowStart + ":" + col + rowEnd).build())
                    .header(AppConstants.AUTHORIZATION, AppConstants.BEARER + token)
                    .retrieve().body(SheetRowsDto.class);
            Map<String, Double> cardMap = new HashMap<>();

            if (response.getValues() != null && !response.getValues().isEmpty()) {
                for (int i = 0; i < CATEGORIES.length; i++) {
                    cardMap.put(CATEGORIES[i].toLowerCase(), Double.parseDouble(response.getValues().get(i).get(0)));
                }
            }
            return cardMap;
        } catch (RestClientResponseException e) {
            throw new GoogleSheetsException("Failed to fetch current month expenses", e);
        }
    }

    private SpreadsheetCreationDto getCreateSpreadsheetPayload() {
        SpreadsheetCreationDto sheetCreationDto = SpreadsheetCreationDto.builder()
                .properties(
                        SpreadsheetCreationDto.Properties.builder()
                                .title("2026 Expenditure")
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
