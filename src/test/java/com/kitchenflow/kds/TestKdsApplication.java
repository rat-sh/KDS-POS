package com.kitchenflow.kds;

import org.springframework.boot.SpringApplication;

public class TestKdsApplication {

	public static void main(String[] args) {
		SpringApplication.from(KdsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
