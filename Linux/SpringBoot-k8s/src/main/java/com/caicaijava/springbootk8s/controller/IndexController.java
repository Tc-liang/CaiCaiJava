package com.caicaijava.springbootk8s.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/11/25 13:32
 * @description:
 */
@RestController
public class IndexController {
    @GetMapping("/index")
    public String index(){
        return "Hello CaiCai";
    }
}
