package com.milind.lazypanel.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/gugugaga")
    public String get() {
        return "PAGE";
    }



    @GetMapping("/gag")
    public String b() {
        return "Logged in";
    }
}