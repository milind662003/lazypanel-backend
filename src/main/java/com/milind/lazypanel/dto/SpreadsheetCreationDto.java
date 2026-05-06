package com.milind.lazypanel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class SpreadsheetCreationDto {

    public Properties properties;
    public ArrayList<Sheet> sheets;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Properties{
        public String title;
        public Integer sheetId;
        public Integer index;
    }


    @Data
    @Builder
    public static class Sheet{
        public Properties properties;
    }
}
