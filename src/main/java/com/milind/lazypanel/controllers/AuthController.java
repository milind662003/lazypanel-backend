package com.milind.lazypanel.controllers;

import com.milind.lazypanel.models.User;
import com.milind.lazypanel.services.implementations.AuthService;
import com.milind.lazypanel.util.CookieUtility;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user, HttpServletResponse response) {
        authService.logout(user.getId());

        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtility.createAccessTokenCookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

}