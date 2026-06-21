package com.milind.lazypanel.controllers;

import com.milind.lazypanel.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {
    //To fetch user information on successful login
    @GetMapping("/userInfo")
    public ResponseEntity<String> getUserInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.OK).body(user.getFirstName());
    }
}
