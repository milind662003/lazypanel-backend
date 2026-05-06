package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

@Data
@AllArgsConstructor
public class SheetsRowsDto {
    public ArrayList<ArrayList<String>> values;
}
