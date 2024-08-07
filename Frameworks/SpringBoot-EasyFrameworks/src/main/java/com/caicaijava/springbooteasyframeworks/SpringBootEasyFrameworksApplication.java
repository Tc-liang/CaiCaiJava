package com.caicaijava.springbooteasyframeworks;

import com.caicaijava.springbooteasyframeworks.spring.controller.TestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.caicaijava.common","com.caicaijava.springbooteasyframeworks"})
public class SpringBootEasyFrameworksApplication {


	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SpringBootEasyFrameworksApplication.class, args);

		TestController testController = context.getBean(TestController.class);

		//原型模式 失效
		for (int i = 0; i < 10; i++) {
			System.out.println(testController.prototype());
		}

	}

}
