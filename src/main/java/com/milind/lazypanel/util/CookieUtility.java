package com.milind.lazypanel.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtility {
    private CookieUtility() {
    }

    public static ResponseCookie createAccessTokenCookie(
            String jwt,
            Duration maxAge) {

        return ResponseCookie.from("access_token", jwt)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
