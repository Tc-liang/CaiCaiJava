package com.caicaijava.springbooteasyframeworks.spring.service;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author 菜菜的后端私房菜
 * @create: 2024/7/18 14:51
 * @description:
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrototypeService {
}
