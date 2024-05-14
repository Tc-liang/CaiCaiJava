package com.caicaijava.springbootwebserver.controller;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/5/11 14:30
 * @description:
 */
@ServerEndpoint("/ws/{id}")
@Component
public class WebSocketServer {

    private static final Map<Long, Session> map = new ConcurrentHashMap<>();

    @OnOpen
    public void open(@PathParam("id") Long id, Session session) {
        map.put(id, session);
        System.out.println(id + " 建立连接");
    }

    @OnClose
    public void close(@PathParam("id") Long id) {
        map.remove(id);
        System.out.println(id + " 关闭连接");
    }

    @OnMessage
    public void msg(String msg) {
        System.out.println("收到消息:" + msg);
    }

}
