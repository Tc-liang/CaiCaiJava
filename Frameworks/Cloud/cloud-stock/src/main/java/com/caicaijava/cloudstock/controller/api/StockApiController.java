package com.caicaijava.cloudstock.controller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/11/1 9:40
 * @description:
 */
@RestController
@RequestMapping("/stockApi")
public class StockApiController {

    @GetMapping("/ded/{id}")
    public Long ded(@PathVariable("id") Long id) {
        return dedStockBySkuId(id, 1);
    }


    private final Map<Long /*id*/, AtomicLong /*stock*/> skuStock = new ConcurrentHashMap<>(16);

    /**
     * @param id     商品ID
     * @param dedNum 扣减数量
     * @return
     */
    private Long dedStockBySkuId(Long id, long dedNum) {
        //如果不存在就创建 库存数量为id值
        AtomicLong stock = skuStock.computeIfAbsent(id, AtomicLong::new);
        long stockNum;
        do {
            stockNum = stock.get();
            if (stockNum <= 0) return -1L;
        } while (!stock.compareAndSet(stockNum, stockNum - dedNum));
        return stockNum - 1;
    }
}
