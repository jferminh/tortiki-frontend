package com.tortiki.frontend.config;

import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import feign.Client;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import feign.httpclient.ApacheHttpClient;
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
 *   <li>Client HTTP Apache remplaçant le client JDK par défaut, pour
 *       supporter la méthode {@code PATCH} (correctif Issue #XX).</li>
 * </ul>
 *
 * <p>Correctif Issue 53 : sans un {@link Encoder} explicite, Feign utilise
 * {@code SpringEncoder} (JSON uniquement), qui sérialise un
 * {@code MultipartFile} en appelant sa méthode {@code toString()} au lieu
 * de transmettre son contenu binaire. {@link SpringFormEncoder} décore
 * {@code SpringEncoder} pour détecter automatiquement les méthodes
 * {@code consumes = "multipart/form-data"} et basculer sur un encodage
 * multipart réel — les appels JSON existants ne sont pas affectés.</p>
 *
 * <p>Correctif Issue #XX : {@code feign.Client$Default}, basé sur
 * {@code java.net.HttpURLConnection}, rejette la méthode {@code PATCH}
 * avec {@code ProtocolException: Invalid HTTP method: PATCH} — limitation
 * historique du JDK, dont la liste de méthodes HTTP autorisées est fermée.
 * {@link ApacheHttpClient} (Apache HttpClient5) supporte nativement
 * {@code PATCH}, {@code PUT} et {@code DELETE} avec corps, sans
 * contournement. Ce bean explicite prime sur toute auto-configuration
 * Spring Cloud, garantissant une traçabilité complète du choix.</p>
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

  /**
   * Client HTTP Feign basé sur Apache HttpClient5.
   *
   * <p>Remplace explicitement {@code feign.Client$Default}, incapable
   * d'exécuter des requêtes {@code PATCH} en raison d'une limitation du
   * JDK ({@code java.net.HttpURLConnection}). Utilisé notamment par
   * {@code ContactApiClient#updateStatus} pour confirmer ou refuser une
   * demande de contact depuis le dashboard vendeur.</p>
   *
   * @return le client Feign basé sur Apache HttpClient5
   */
  @Bean
  public Client feignClient() {
    return new ApacheHttpClient();
  }
}