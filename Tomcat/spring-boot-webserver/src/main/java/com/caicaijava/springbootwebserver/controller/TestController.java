package com.caicaijava.springbootwebserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/24 15:59
 * @description:
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @RequestMapping("/add")
    public String add(HttpServletRequest request) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        return "add";
    }
}
