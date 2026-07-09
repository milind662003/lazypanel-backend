package com.milind.lazypanel.controllers;

import com.milind.lazypanel.dto.AddExpenseRequestDto;
import com.milind.lazypanel.dto.SheetStatusResponse;
import com.milind.lazypanel.dto.SheetsResponseDto;
import com.milind.lazypanel.models.User;
import com.milind.lazypanel.services.interfaces.IGoogleSheetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;

@RestController
public class GoogleSheetsController {

    @Autowired
    private IGoogleSheetsService googleSheetsService;

    @GetMapping("/getSheetStatus")
    public ResponseEntity<SheetStatusResponse> getSheetStatus(@AuthenticationPrincipal User user) {
        SheetStatusResponse response = googleSheetsService.getSheetStatus(user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/addExpense")
    public ResponseEntity<SheetsResponseDto> appendRowToSheet(@AuthenticationPrincipal User user,
                                                              @RequestBody ArrayList<AddExpenseRequestDto> expenses) {
        SheetsResponseDto response = googleSheetsService.appendRowToSheet(user.getId(), expenses);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/createSheet")
    public ResponseEntity<SheetsResponseDto> createSheet(@AuthenticationPrincipal User user) {
        SheetsResponseDto response = googleSheetsService.createAndSetupSheet(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getDetailsForCard")
    public ResponseEntity<Map<String, Double>> getCurrentMonthTotal(@AuthenticationPrincipal User user) {
        Map<String, Double> response = googleSheetsService.getCurrentMonthExpenses(user.getId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
