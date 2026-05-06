package com.milind.lazypanel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class SpreadsheetSetupDto {
    public ArrayList<Request> requests;
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cell {
        public UserEnteredValue userEnteredValue;
        public UserEnteredFormat userEnteredFormat;
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Condition {
        public String type;
        public ArrayList<Value> values;
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Range {
        public Integer sheetId;
        public Integer startRowIndex;
        public Integer endRowIndex;
        public Integer startColumnIndex;
        public Integer endColumnIndex;
    }
    @Data
    @Builder
    public static class RepeatCell {
        public Range range;
        public Cell cell;
        @Builder.Default
        public String fields = "userEnteredValue";
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Request {
        public UpdateCells updateCells;
        public RepeatCell repeatCell;
        public SetDataValidation setDataValidation;
    }
    @Data
    @Builder
    public static class Row {
        public ArrayList<Value> values;
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Rule {
        public Condition condition;
        @Builder.Default
        public boolean showCustomUi = true;
        public boolean strict;
    }
    @Data
    @Builder
    public static class SetDataValidation {
        public Range range;
        public Rule rule;
    }
    @Data
    @Builder
    public static class UpdateCells {
        public Range range;
        public ArrayList<Row> rows;
        @Builder.Default
        public String fields = "userEnteredValue";
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserEnteredValue {
        public String stringValue;
        public String formulaValue;
    }
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Value {
        public UserEnteredValue userEnteredValue;
        public String userEnteredValueString;

        @com.fasterxml.jackson.annotation.JsonProperty("userEnteredValue")
        public Object getEffectiveValue() {
            return userEnteredValue != null ? userEnteredValue : userEnteredValueString;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public UserEnteredValue getUserEnteredValue() {
            return userEnteredValue;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public String getUserEnteredValueString() {
            return userEnteredValueString;
        }
    }

    @Data
    @Builder
    public static class TextFormat{
        @Builder.Default
        public boolean bold = true;
    }

    @Data
    @Builder
    public static class UserEnteredFormat{
        public TextFormat textFormat;
    }
}
