package com.caicaijava.springbooteasyframeworks.spring.service;

import org.springframework.stereotype.Service;

/**
 * @author 菜菜的后端私房菜
 * @create: 2024/7/18 14:31
 * @description: 定义Bean的有参构造，spring在构造时会自动在容器中寻找参数（多个相同类型的Bean根据名称搜索），找不到则调用无参构造，没有无参构造报错
 */
@Service
public class ConstructService {

    private String name;

    //没有String的Bean 或 没有无参构造 报错
    public ConstructService(String name) {
        this.name = name;
    }

    public ConstructService(){

    }
}
