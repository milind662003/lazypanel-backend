package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor // Good practice to have this for Jackson
public class SheetsResponseDto {
    private String spreadsheetId;
    private Integer totalUpdatedRows;
    private Integer totalUpdatedColumns;
    private Integer totalUpdatedCells;
    private Integer totalUpdatedSheets;
    private List<UpdateValuesResponse> responses;

    public SheetsResponseDto(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateValuesResponse {
        private String spreadsheetId;
        private String updatedRange;
        private Integer updatedRows;
        private Integer updatedColumns;
        private Integer updatedCells;
    }
}