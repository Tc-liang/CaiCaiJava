package com.caicaijava.springbooteasyframeworks.controller;

import com.caicaijava.common.CommonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/7/10 16:00
 * @description:
 */

@RequestMapping("/demo")
@RestController
public class DemoController {

    @GetMapping("/demo")
    public String demo(){
        return "demo";
    }

    @Autowired
    private CommonConfig commonConfig;

    @GetMapping("/test")
    public String test() {
        String config = commonConfig.commonConfig();
        System.out.println(config);
        return "test" + config;
    }

}
