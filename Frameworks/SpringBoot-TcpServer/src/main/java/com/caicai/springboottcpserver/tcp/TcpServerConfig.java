package com.caicai.springboottcpserver.tcp;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;

@Configuration
@EnableIntegration
@Slf4j
public class TcpServerConfig {

    @Bean
    public ByteArrayCrLfSerializer serializer() {
        return new ByteArrayCrLfSerializer();
    }

    @Bean
    public SmokeDeviceTcpTransformer transformer() {
        return new SmokeDeviceTcpTransformer();
    }

    @Bean
    public TcpNetServerConnectionFactory serverConnectionFactory() {
        // 设置端口号
        TcpNetServerConnectionFactory factory = new TcpNetServerConnectionFactory(1234);
        // 设置反序列化器
        factory.setDeserializer(serializer());
        // 设置序列化器
        factory.setSerializer(serializer());
        return factory;
        //todo cl 发送 HTTP MQTT
    }

    @Bean
    public IntegrationFlow tcpInboundFlow() {
        return IntegrationFlows.from(Tcp.inboundAdapter(serverConnectionFactory()))
                // 消息转换
                .transform(transformer())
                // 处理消息
                .handle(message -> {
                    System.out.println(message);
                    String jsonString = JSON.toJSONString(message.getPayload());
                    log.info("准备发送消息体JSON:{}", jsonString);
                })
                .get();
    }
}