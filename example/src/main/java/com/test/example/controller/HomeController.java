package com.test.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Ryan
 */
@Controller
public class HomeController {

    @GetMapping(value = {"/api", "/"})
    public String api() {
        return "redirect:/swagger-ui.html";
    }

}
