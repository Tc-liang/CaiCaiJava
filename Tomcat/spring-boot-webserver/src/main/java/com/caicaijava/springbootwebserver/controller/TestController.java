package com.caicaijava.springbootwebserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @GetMapping("/cpu")
    public int cpu(){
        int res = 0;
        for (int index = 0; index < 100000; index++) {
            res++;
        }
        System.out.println("CPU完成");
        return res;
    }

    @GetMapping("/io")
    public int io(HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
        int res = 0;
        HttpSession session = request.getSession();
        Thread.sleep(500);
        System.out.println("IO完成");
        return res;
    }

    @GetMapping("/sleep")
    public int sleep() throws InterruptedException {
        int res = 0;
        Thread.sleep(5000);
        System.out.println("sleep完成");
        return res;
    }
}
