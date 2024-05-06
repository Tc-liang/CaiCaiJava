package com.caicaijava.springbootwebserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author: cl
 * @create: 2024/4/24 15:59
 * @description:
 */
@RestController
@RequestMapping("/test")
public class TestController {
    @RequestMapping("/add")
    public String add(HttpServletRequest request){
        HttpSession session = request.getSession();

        return "add";
    }
}
