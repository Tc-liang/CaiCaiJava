package com.caicaijava.springbootai.controller;

import com.caicaijava.springbootai.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIController {

    @Autowired
    private AIService aiService;

    @GetMapping("/msg")
    public String sendMessage(String message) {
        return aiService.chat(message);
    }
}