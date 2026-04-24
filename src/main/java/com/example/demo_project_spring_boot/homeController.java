package com.example.demo_project_spring_boot;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class homeController {

    @RequestMapping("/")
    public String home(HttpServletRequest request) {
        return "Hello World" + request.getSession().getId();
    }
}
