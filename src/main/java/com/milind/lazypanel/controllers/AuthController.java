package com.milind.lazypanel.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/gugugaga")
    public String get() {
        return "PAGE";
    }

    @GetMapping("/")
    public String f() {
        return "Logged in";
    }


    @GetMapping("/gag")
    public String b() {
        return "Logged in";
    }
}
