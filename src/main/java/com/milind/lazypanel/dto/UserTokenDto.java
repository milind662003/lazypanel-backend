package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class UserTokenDto {
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
}
