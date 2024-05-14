package com.caicaijava.springbootwebserver.config;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.apache.coyote.ajp.AjpNio2Protocol;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Objects;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/24 16:12
 * @description:
 */
@Configuration
public class WebServerConfig {

    /**
     * 增强WEB服务器工厂
     *
     * @return TomcatServletWebServerFactory
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatServletWebServerFactory() {
        return serverFactory -> {
            if (Objects.nonNull(serverFactory)) {
                System.out.println("后置处理器增强工厂");
            }
        };
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = tomcatFactory();
        System.out.println("创建服务器工厂结束...");
        return factory;
//        return jettyFactory();
    }

    private static JettyServletWebServerFactory jettyFactory() {
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        factory.setPort(8080);
        return factory;
    }

    private TomcatServletWebServerFactory tomcatFactory() {
        TomcatServletWebServerFactory serverFactory = new TomcatServletWebServerFactory(8080);
        //添加其他连接器，后续创建Tomcat时会把连接器加到所有service中
        serverFactory.addAdditionalTomcatConnectors(HTTPConnector());
        serverFactory.addAdditionalTomcatConnectors(AJPConnector());

        //添加容器pipe中的Valve扩展
        serverFactory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                System.out.println("engine valve 扩展");
                //记得继续调用
                getNext().invoke(request, response);
            }
        });

        serverFactory.addContextValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                System.out.println("context valve 扩展");
                //记得继续调用
                getNext().invoke(request, response);
            }
        });

        //context容器 监听器
        serverFactory.addContextLifecycleListeners(event -> {
            System.out.println("context生命周期监听器:");
            System.out.println("type:" + event.getType());
            System.out.println("data:" + event.getData());
            System.out.println("life cycle:" + event.getLifecycle());
        });

        return serverFactory;
    }


    private Connector HTTPConnector() {
        Connector connector = new Connector(new Http11Nio2Protocol());
        connector.setScheme("http");
        connector.setPort(8888);
        connector.setAllowTrace(false);
        connector.setSecure(false);
        return connector;
    }

    private Connector AJPConnector() {
        Connector connector = new Connector(new AjpNio2Protocol());
        connector.setScheme("ajp");
        connector.setPort(6666);
        ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setSecret("nb 666");
        return connector;
    }
}