package com.caicai;

import java.util.ServiceLoader;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2025/1/5 10:25
 * @description:
 */
public class SPIDemo {
    public static void main(String[] args) {
        ServiceLoader<DatabaseInterface> serviceLoader = ServiceLoader.load(DatabaseInterface.class);
        for (DatabaseInterface databaseInterface : serviceLoader) {
            System.out.println("使用的数据库:" + databaseInterface.getDatabaseName());
        }
    }
}
