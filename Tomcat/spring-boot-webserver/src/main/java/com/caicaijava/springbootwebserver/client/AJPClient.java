package com.caicaijava.springbootwebserver.client;

import com.github.jrialland.ajpclient.impl.CPingImpl;
import com.github.jrialland.ajpclient.pool.Channels;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

public class AJPClient {
    public static void main(String[] args) throws Exception {
        final Channel channel = Channels.connect("localhost", 6666);
        boolean success = new CPingImpl(2, TimeUnit.SECONDS).execute(channel);
        System.out.println(success);
    }
}