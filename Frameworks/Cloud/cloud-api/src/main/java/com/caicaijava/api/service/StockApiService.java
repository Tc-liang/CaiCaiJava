package com.caicaijava.api.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/11/1 9:39
 * @description:
 */
@FeignClient(name = "cloud-stock")
public interface StockApiService {
    @GetMapping("/stockApi/ded/{id}")
    Long ded(@PathVariable("id") Long id);
}
