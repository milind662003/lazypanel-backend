package com.milind.lazypanel.controller;

import com.milind.lazypanel.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserInfoController {
    //To fetch user information on successful login
    @GetMapping
    public ResponseEntity<String> getUserInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.OK).body(user.getFirstName());
    }
}
