package com.tortiki.frontend.config;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;

/**
 * Configuration du filtre de résolution multipart, positionné en amont
 * de la chaîne de filtres Spring Security.
 *
 * <p>Problème résolu : {@code CsrfFilter} appelle {@code request.getParameter()}
 * pour lire le jeton CSRF, ce qui force Tomcat à parser nativement le flux
 * multipart. Ce parsing natif consomme le flux HTTP une première fois ;
 * toute tentative ultérieure de lecture (ex. {@code HiddenHttpMethodFilter}
 * lors d'un forward vers {@code /error}) lève une
 * {@code FileCountLimitExceededException} car le flux est déjà épuisé.</p>
 *
 * <p>En enregistrant {@link MultipartFilter} avec la précédence la plus haute,
 * Spring résout le multipart une seule fois via son propre résolveur
 * ({@code StandardServletMultipartResolver}), avant que {@code CsrfFilter}
 * n'accède aux paramètres. Le jeton {@code _csrf} devient alors un paramètre
 * de requête standard, lisible sans nouveau parsing.</p>
 *
 * <p>Référence officielle Spring Security — section CSRF et upload multipart.</p>
 */
@Configuration
public class MultipartConfig {

  /**
   * Enregistre {@link MultipartFilter} avec la priorité la plus haute
   * de la chaîne de filtres servlet, avant {@code springSecurityFilterChain}.
   *
   * @return l'enregistrement du filtre multipart
   */
  @Bean
  public FilterRegistrationBean<Filter> multipartFilterRegistration() {
    final FilterRegistrationBean<Filter> registration =
        new FilterRegistrationBean<>(new MultipartFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }
}