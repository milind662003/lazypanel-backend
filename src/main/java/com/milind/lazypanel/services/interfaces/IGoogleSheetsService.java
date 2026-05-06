package com.milind.lazypanel.services.interfaces;

import com.milind.lazypanel.dto.SheetsResponseDto;

import java.util.ArrayList;

public interface IGoogleSheetsService {

    SheetsResponseDto getSheetDetails(String jwt);

    SheetsResponseDto appendRowToSheet(String jwt, ArrayList<ArrayList<String>> rows);

    SheetsResponseDto createAndSetupSheet(String jwt);
}
