package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshTokenResponseDto {
    private String access_token;
    private int expires_in;
    private int refresh_token_expires_in;
}
