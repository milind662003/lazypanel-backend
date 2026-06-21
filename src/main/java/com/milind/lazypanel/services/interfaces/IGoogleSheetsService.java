package com.milind.lazypanel.services.interfaces;

import com.milind.lazypanel.dto.AddExpenseRequestDto;
import com.milind.lazypanel.dto.SheetsResponseDto;

import java.util.ArrayList;
import java.util.Map;

public interface IGoogleSheetsService {

    SheetsResponseDto getSheetDetails(Long userId);

    SheetsResponseDto appendRowToSheet(Long userId, ArrayList<AddExpenseRequestDto> expenses);

    SheetsResponseDto createAndSetupSheet(Long userId);

    Map<String, Double> getCurrentMonthExpenses(Long userId);
}
