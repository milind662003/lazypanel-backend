package com.milind.lazypanel.service.interfaces;

import com.milind.lazypanel.dto.AddExpenseRequestDto;
import com.milind.lazypanel.dto.SheetStatusResponse;
import com.milind.lazypanel.dto.SheetsResponseDto;
import com.milind.lazypanel.model.User;

import java.util.ArrayList;
import java.util.Map;

public interface GoogleSheetsService {

    SheetStatusResponse getSheetStatus(Long userId);

    SheetsResponseDto appendRowToSheet(Long userId, ArrayList<AddExpenseRequestDto> expenses);

    SheetsResponseDto createAndSetupSheet(User user);

    Map<String, Double> getCurrentMonthExpenses(Long userId);
}
