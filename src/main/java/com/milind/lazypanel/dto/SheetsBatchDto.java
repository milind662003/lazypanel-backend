package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

public class SheetsBatchDto {
    @Data
    @AllArgsConstructor
    public static class ValueRange {
        private String range;
        private String majorDimension;
        private List<List<String>> values;
    }

    @Data
    public static class GetResponse {
        private String spreadsheetId;
        private List<ValueRange> valueRanges;
    }

    @Data
    @AllArgsConstructor
    public static class UpdateRequest {
        private String valueInputOption;
        private List<ValueRange> data;
    }
}