package com.example.Docker.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		System.out.println("DB_USERNAME=" + System.getenv("DB_USERNAME"));
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
