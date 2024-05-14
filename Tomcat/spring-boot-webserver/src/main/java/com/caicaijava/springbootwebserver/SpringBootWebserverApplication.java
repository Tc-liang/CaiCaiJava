package com.caicaijava.springbootwebserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableWebSocket
@SpringBootApplication
public class SpringBootWebserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebserverApplication.class, args);
	}

}
