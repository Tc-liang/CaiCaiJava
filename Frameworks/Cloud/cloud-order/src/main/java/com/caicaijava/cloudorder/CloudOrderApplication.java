package com.caicaijava.cloudorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.caicaijava.api","com.caicaijava.cloudorder"})
@EnableFeignClients(basePackages = "com.caicaijava.api")
public class CloudOrderApplication {
	public static void main(String[] args) {
		SpringApplication.run(CloudOrderApplication.class, args);
	}

}
