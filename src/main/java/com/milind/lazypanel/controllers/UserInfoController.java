package com.milind.lazypanel.controllers;

import com.milind.lazypanel.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {
    //To fetch user information on successful login
    @GetMapping("/userInfo")
    public ResponseEntity<String> getUserInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.OK).body(user.getFirstName());
    }
}
