package com.tortiki.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Point d'entrée de l'application web Tortiki Frontend.
 *
 * <p>Active Spring Cloud OpenFeign pour la communication avec tortiki-api.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class TortikiFrontendApplication {

  /**
   * Démarre l'application Spring Boot Tortiki Frontend.
   *
   * @param args arguments de ligne de commande
   */
  public static void main(final String[] args) {
    SpringApplication.run(TortikiFrontendApplication.class, args);
  }
}