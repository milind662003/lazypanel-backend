package com.milind.lazypanel.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AddExpenseRequestDto {
    private String description;
    private LocalDate date;
    private double amount;
    private String category;
}
