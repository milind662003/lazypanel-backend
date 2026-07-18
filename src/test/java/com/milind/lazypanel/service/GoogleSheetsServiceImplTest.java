package com.milind.lazypanel.service;

import com.milind.lazypanel.dto.SheetStatusResponse;
import com.milind.lazypanel.dto.SheetsResponseDto;
import com.milind.lazypanel.exception.ResourceNotFoundException;
import com.milind.lazypanel.model.User;
import com.milind.lazypanel.model.UserSheet;
import com.milind.lazypanel.repository.SheetRepository;
import com.milind.lazypanel.service.implementations.GoogleSheetsServiceImpl;
import com.milind.lazypanel.service.implementations.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsServiceImplTest {

    @Mock
    private SheetRepository sheetRepository;

    @Mock
    private TokenServiceImpl tokenService;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    private GoogleSheetsServiceImpl googleSheetsService;

    @BeforeEach
    void setUp() {

        when(restClientBuilder.baseUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(restClientBuilder);

        when(restClientBuilder.build())
                .thenReturn(restClient);

        googleSheetsService = new GoogleSheetsServiceImpl(restClientBuilder);

        ReflectionTestUtils.setField(
                googleSheetsService,
                "sheetRepository",
                sheetRepository
        );

        ReflectionTestUtils.setField(
                googleSheetsService,
                "tokenService",
                tokenService
        );
    }

    @Test
    void shouldReturnConfiguredTrueWhenSheetExists() {

        when(sheetRepository.findByUserId(1L))
                .thenReturn(UserSheet.builder().build());

        SheetStatusResponse response =
                googleSheetsService.getSheetStatus(1L);

        assertTrue(response.isConfigured());
    }

    @Test
    void shouldReturnConfiguredFalseWhenSheetDoesNotExist() {

        when(sheetRepository.findByUserId(1L))
                .thenReturn(null);

        SheetStatusResponse response =
                googleSheetsService.getSheetStatus(1L);

        assertFalse(response.isConfigured());
    }

    @Test
    void shouldThrowWhenAppendingRowsWithoutConfiguredSheet() {

        when(sheetRepository.findByUserId(1L))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleSheetsService.appendRowToSheet(
                        1L,
                        new ArrayList<>()
                )
        );
    }

    @Test
    void shouldReturnExistingSpreadsheetWhenAlreadyConfigured() {

        UserSheet sheet = UserSheet.builder()
                .spreadsheetId("spreadsheet-123")
                .build();

        when(sheetRepository.findByUserId(1L))
                .thenReturn(sheet);

        SheetsResponseDto response =
                googleSheetsService.createAndSetupSheet(
                        User.builder().id(1L).build()
                );

        assertEquals("spreadsheet-123", response.getSpreadsheetId());
    }

    @Test
    void shouldThrowWhenFetchingExpensesWithoutConfiguredSheet() {

        when(sheetRepository.findByUserId(1L))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleSheetsService.getCurrentMonthExpenses(1L)
        );
    }
}
