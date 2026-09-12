package com.example.spring_boot_project_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootProjectApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootProjectApiApplication.class, args);
	}

}

// @SpringBootApplication
// public class YourApplication {
//     public static void main(String[] args) {
//         SpringApplication app = new SpringApplication(YourApplication.class);
//         var context = app.run(args);
//         System.out.println("DB_HOST = " + context.getEnvironment().getProperty("DB_HOST"));
//         System.out.println("DB_NAME = " + context.getEnvironment().getProperty("DB_NAME"));
//     }
// }