package com.caicaijava.cloudorder.controller;

import com.caicaijava.api.service.StockApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/10/31 17:35
 * @description:
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private StockApiService stockApiService;

    private final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/pay/{id}")
    public String pay(@PathVariable("id") Long id) {
        //订单状态更改..
        updateOrderStatus(id);

        //扣减库存..
        Long stockNum = stockApiService.ded(id);
        if (stockNum < 0L) {
            return "支付失败,库存不足";
        }
        return "支付成功,剩余库存:" + stockNum;
    }

    private void updateOrderStatus(Long id) {
        logger.info("{}订单状态更改..", id);
    }

}
