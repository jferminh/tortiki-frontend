package com.tortiki.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Point d'entrée de l'application web Tortiki Frontend.
 * <p>Active Spring Cloud OpenFeign pour la communication avec tortiki-api.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class TortikiFrontendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TortikiFrontendApplication.class, args);
	}
}