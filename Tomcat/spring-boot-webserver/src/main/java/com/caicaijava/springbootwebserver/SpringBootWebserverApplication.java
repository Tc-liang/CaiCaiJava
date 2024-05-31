package com.caicaijava.springbootwebserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableWebSocket
@SpringBootApplication
@ServletComponentScan
public class SpringBootWebserverApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebserverApplication.class, args);
	}
}
