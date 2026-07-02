package com.tortiki.frontend.config;

import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
 *   <li>Propagation du cookie de session {@code tortiki-api}
 *       (stocké en session frontend par {@code ApiDelegatingAuthenticationProvider})
 *       vers chaque requête Feign sortante.</li>
 *   <li>Niveau de log Feign adapté au profil actif (BASIC en dev, NONE en prod).</li>
 * </ul>
 *
 * <p>Important : ce n'est plus le cookie {@code JSESSIONID} du navigateur
 * (session frontend) qui est relayé, mais le cookie de session de l'API
 * lui-même, obtenu lors du login et conservé côté serveur. Les deux sessions
 * (frontend et API) sont distinctes par nature — voir Issue 58.</p>
 */
@Slf4j
@Configuration
public class FeignConfig {

  /** Nom de l'en-tête HTTP transportant les cookies de session. */
  private static final String HEADER_COOKIE = "Cookie";

  /**
   * Intercepteur Feign qui relaie le cookie de session {@code tortiki-api}
   * (stocké en session frontend lors du login) vers chaque requête sortante.
   *
   * @return intercepteur ajoutant l'en-tête {@code Cookie} si présent
   */
  @Bean
  public RequestInterceptor sessionCookieInterceptor() {
    return requestTemplate -> {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        log.debug("Aucun contexte de requête HTTP actif — appel Feign hors requête web.");
        return;
      }
      HttpServletRequest request = attributes.getRequest();
      HttpSession session = request.getSession(false);
      if (session == null) {
        log.debug("Aucune session HTTP active — requête Feign anonyme.");
        return;
      }
      Object apiCookie = session.getAttribute(
          ApiDelegatingAuthenticationProvider.SESSION_ATTR_API_COOKIE);
      if (apiCookie instanceof String cookieValue && !cookieValue.isBlank()) {
        requestTemplate.header(HEADER_COOKIE, cookieValue);
        log.debug("Cookie de session API propagé : {}",
            cookieValue.replaceAll("JSESSIONID=[^;]+", "JSESSIONID=***"));
      } else {
        log.debug("Aucun cookie de session API en session frontend.");
      }
    };
  }

  /**
   * Niveau de log Feign.
   *
   * <p>{@code BASIC} en développement pour diagnostiquer les requêtes/réponses.
   * Surchargé en {@code NONE} via {@code application-prod.yml}.</p>
   *
   * @return le niveau de log Feign
   */
  @Bean
  public Logger.Level feignLogLevel() {
    return Logger.Level.BASIC;
  }
}