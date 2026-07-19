package com.milind.lazypanel.controller;

import com.milind.lazypanel.dto.AddExpenseRequestDto;
import com.milind.lazypanel.dto.SheetStatusResponse;
import com.milind.lazypanel.dto.SheetsResponseDto;
import com.milind.lazypanel.model.User;
import com.milind.lazypanel.service.interfaces.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class GoogleSheetsController {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @GetMapping("/status")
    public ResponseEntity<SheetStatusResponse> getSheetStatus(@AuthenticationPrincipal User user) {
        SheetStatusResponse response = googleSheetsService.getSheetStatus(user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<SheetsResponseDto> appendRowToSheet(@AuthenticationPrincipal User user,
                                                              @RequestBody ArrayList<AddExpenseRequestDto> expenses) {
        SheetsResponseDto response = googleSheetsService.appendRowToSheet(user.getId(), expenses);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/sheet")
    public ResponseEntity<SheetsResponseDto> createSheet(@AuthenticationPrincipal User user) {
        SheetsResponseDto response = googleSheetsService.createAndSetupSheet(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/card")
    public ResponseEntity<Map<String, Double>> getCurrentMonthTotal(@AuthenticationPrincipal User user) {
        Map<String, Double> response = googleSheetsService.getCurrentMonthExpenses(user.getId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
