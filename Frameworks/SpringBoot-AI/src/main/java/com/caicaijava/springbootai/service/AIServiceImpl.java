package com.caicaijava.springbootai.service;

import com.alibaba.cloud.ai.tongyi.image.TongYiImagesModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/11/7 9:54
 * @description:
 */
@Component
public class AIServiceImpl implements AIService {

    private final ChatModel chatModel;


    @Autowired
    public AIServiceImpl(ChatModel TongYiChatClient) {
        this.chatModel = TongYiChatClient;
    }

    @Override
    public String chat(String msg) {
        Prompt prompt = new Prompt(new UserMessage(msg));
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

}
