package com.caicai;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2025/1/5 10:31
 * @description:
 */
public class PgSQLDatabase implements DatabaseInterface{
    @Override
    public String getDatabaseName() {
        return "PgSQL";
    }
}
