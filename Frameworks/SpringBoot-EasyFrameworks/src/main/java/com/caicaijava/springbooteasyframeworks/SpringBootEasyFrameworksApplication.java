package com.caicaijava.springbooteasyframeworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.caicaijava.common","com.caicaijava.springbooteasyframeworks"})
public class SpringBootEasyFrameworksApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootEasyFrameworksApplication.class, args);
	}

}
