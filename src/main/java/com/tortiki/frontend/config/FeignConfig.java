package com.tortiki.frontend.config;

import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration de Spring Cloud OpenFeign pour le frontend Tortiki.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Propagation du cookie de session {@code tortiki-api} vers chaque
 *       requête Feign sortante.</li>
 *   <li>Encodage {@code multipart/form-data} pour l'upload de photo
 *       (Issue 53 — correctif aval).</li>
 *   <li>Niveau de log Feign adapté au profil actif (BASIC en dev, NONE en prod).</li>
 * </ul>
 *
 * <p>Correctif Issue 53 : sans un {@link Encoder} explicite, Feign utilise
 * {@code SpringEncoder} (JSON uniquement), qui sérialise un
 * {@code MultipartFile} en appelant sa méthode {@code toString()} au lieu
 * de transmettre son contenu binaire. {@link SpringFormEncoder} décore
 * {@code SpringEncoder} pour détecter automatiquement les méthodes
 * {@code consumes = "multipart/form-data"} et basculer sur un encodage
 * multipart réel — les appels JSON existants ne sont pas affectés.</p>
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
   * Encodeur Feign décoré pour supporter {@code multipart/form-data}.
   *
   * <p>{@link SpringFormEncoder} enveloppe {@link SpringEncoder} : il délègue
   * à ce dernier pour tous les appels JSON classiques, et active un encodage
   * multipart uniquement pour les méthodes annotées
   * {@code consumes = "multipart/form-data"}, comme
   * {@code ListingApiClient#uploadPhoto}.</p>
   *
   * @param messageConverters fabrique des convertisseurs HTTP Spring,
   *                          nécessaire à la construction de {@link SpringEncoder}
   * @return l'encodeur Feign combiné JSON + multipart
   */
  @Bean
  public Encoder feignEncoder(final ObjectFactory<HttpMessageConverters> messageConverters) {
    return new SpringFormEncoder(new SpringEncoder(messageConverters));
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