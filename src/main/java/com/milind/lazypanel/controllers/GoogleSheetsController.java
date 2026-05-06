package com.milind.lazypanel.controllers;

import com.milind.lazypanel.constants.AppConstants;
import com.milind.lazypanel.dto.SheetsResponseDto;
import com.milind.lazypanel.dto.SheetsRowsDto;
import com.milind.lazypanel.services.interfaces.IGoogleSheetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@RestController
public class GoogleSheetsController {

    @Autowired
    private IGoogleSheetsService googleSheetsService;

    @GetMapping("/sheets")
    public ResponseEntity<SheetsResponseDto> getSheetDetails(
            @RequestHeader(AppConstants.AUTHORIZATION) String authHeader) {
        SheetsResponseDto response = googleSheetsService.getSheetDetails(authHeader.substring(7));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @PostMapping("/addExpense")
    public ResponseEntity<SheetsResponseDto> appendRowToSheet(
            @RequestHeader(AppConstants.AUTHORIZATION) String authHeader,
            @RequestBody ArrayList<ArrayList<String>> rows) {
        SheetsResponseDto response = googleSheetsService.appendRowToSheet(authHeader.substring(7), rows);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/createSheet")
    public ResponseEntity<SheetsResponseDto> createSheet(
            @RequestHeader(AppConstants.AUTHORIZATION) String authHeader) {
        SheetsResponseDto response = googleSheetsService.createAndSetupSheet(authHeader.substring(7));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
