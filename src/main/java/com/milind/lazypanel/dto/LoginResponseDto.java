package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {

    private String jwt;
    private String username;
    private String message;
}
