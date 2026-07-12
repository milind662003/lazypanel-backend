package com.milind.lazypanel.controllers;

import com.milind.lazypanel.models.User;
import com.milind.lazypanel.services.implementations.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user, HttpServletResponse response) {
        authService.logout(user);
        ResponseCookie responseCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(false).path("/").sameSite("Lax").maxAge(0).build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        return ResponseEntity.noContent().build();
    }

}