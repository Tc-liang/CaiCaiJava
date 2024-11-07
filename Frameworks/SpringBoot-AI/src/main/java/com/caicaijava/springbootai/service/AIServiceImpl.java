package com.caicaijava.springbootai.service;

import com.alibaba.cloud.ai.tongyi.chat.TongYiChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/11/7 9:54
 * @description:
 */
@Component
public class AIServiceImpl implements AIService{

    private final TongYiChatModel tongYiChatModel;

    @Autowired
    public AIServiceImpl(TongYiChatModel TongYiChatClient) {
        this.tongYiChatModel = TongYiChatClient;
    }

    @Override
    public String chat(String msg) {
        Prompt prompt = new Prompt(new UserMessage(msg));
        return tongYiChatModel.call(prompt).getResult().getOutput().getContent();
    }
}
