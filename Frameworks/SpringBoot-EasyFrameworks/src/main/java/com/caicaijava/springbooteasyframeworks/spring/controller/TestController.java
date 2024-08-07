package com.caicaijava.springbooteasyframeworks.spring.controller;

import com.caicaijava.springbooteasyframeworks.spring.service.ConstructService;
import com.caicaijava.springbooteasyframeworks.spring.service.PrototypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 菜菜的后端私房菜
 * @create: 2024/7/18 14:32
 * @description:
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ConstructService constructService;

    @RequestMapping("/test")
    public String test() {
        return "test";
    }


    @Autowired
    private PrototypeService prototypeService;

    @RequestMapping("/prototype")
    public String prototype() {
        return prototypeService.toString();
    }
}
