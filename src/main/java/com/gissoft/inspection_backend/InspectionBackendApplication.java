package com.gissoft.inspection_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class InspectionBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InspectionBackendApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logPort(ApplicationReadyEvent event) {
		Environment env = event.getApplicationContext().getEnvironment();
		System.out.println("DB = " + env.getProperty("spring.datasource.url"));
		System.out.println("Server started on port " + env.getProperty("server.port"));
	}
}
