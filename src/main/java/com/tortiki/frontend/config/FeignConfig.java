package com.tortiki.frontend.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration de Spring Cloud OpenFeign pour le frontend Tortiki.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Propagation du cookie {@code JSESSIONID} de l'API vers chaque requête Feign sortante.</li>
 *   <li>Niveau de log Feign adapté au profil actif (FULL en dev, NONE en prod).</li>
 * </ul>
 *
 * <p>Principe : le frontend est un client stateful de l'API. La session Spring Security
 * est ouverte côté API ; le frontend se contente de la relayer via le cookie HTTP.
 * Aucune logique d'authentification ne doit résider dans cette classe.</p>
 */
@Slf4j
@Configuration
public class FeignConfig {

  /**
   * Intercepteur Feign qui relaie le cookie {@code JSESSIONID} reçu du navigateur
   * vers chaque requête HTTP sortante vers {@code tortiki-api}.
   *
   * <p>Sans cet intercepteur, Feign envoie des requêtes anonymes même si
   * l'utilisateur est connecté côté frontend, ce qui provoque des 401.</p>
   *
   * @return l'intercepteur de propagation de session
   */
  @Bean
  public RequestInterceptor sessionCookieInterceptor() {
    return requestTemplate -> {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String cookie = request.getHeader("Cookie");
        if (cookie != null && !cookie.isBlank()) {
          requestTemplate.header("Cookie", cookie);
          log.debug("Cookie de session propagé vers l'API : {}",
              cookie.replaceAll("JSESSIONID=[^;]+", "JSESSIONID=***"));
        }
      }
    };
  }

  /**
   * Niveau de log Feign.
   *
   * <p>{@code FULL} en développement pour diagnostiquer les requêtes/réponses.
   * À surcharger en {@code NONE} via {@code application-prod.yml}.</p>
   *
   * @return le niveau de log Feign
   */
  @Bean
  public Logger.Level feignLogLevel() {
    return Logger.Level.BASIC;
  }
}