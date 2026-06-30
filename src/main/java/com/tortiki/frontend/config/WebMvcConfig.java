package com.tortiki.frontend.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Configuration Spring MVC du frontend Tortiki.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Déclaration des ressources statiques (CSS, JS, images).</li>
 *   <li>Locale française par défaut pour les messages de validation.</li>
 *   <li>Source des messages i18n depuis {@code messages.properties}.</li>
 * </ul>
 *
 * <p>Implémente {@code WebMvcConfigurer} pour étendre la configuration
 * Spring MVC par défaut sans la remplacer — principe OCP.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /** Chemin des ressources statiques dans le classpath. */
  private static final String STATIC_RESOURCES_PATH = "classpath:/static/";

  /** Pattern d'URL pour les fichiers CSS. */
  private static final String ROUTE_CSS = "/css/**";

  /** Pattern d'URL pour les fichiers JavaScript. */
  private static final String ROUTE_JS = "/js/**";

  /** Pattern d'URL pour les images. */
  private static final String ROUTE_IMAGES = "/images/**";

  /** Pattern d'URL pour les webjars (Bootstrap via CDN local). */
  private static final String ROUTE_WEBJARS = "/webjars/**";

  /** Chemin classpath des webjars. */
  private static final String WEBJARS_PATH = "classpath:/META-INF/resources/webjars/";

  /** Encodage des fichiers de messages i18n. */
  private static final String MESSAGE_ENCODING = "UTF-8";

  /** Basename des fichiers de messages (messages.properties). */
  private static final String MESSAGE_BASENAME = "classpath:messages";

  /**
   * Déclare les handlers pour les ressources statiques.
   *
   * <p>Spring Boot auto-configure déjà {@code /static/**} mais cette
   * déclaration explicite garantit l'ordre de résolution et la compatibilité
   * avec le {@code SecurityConfig} qui autorise {@code /css/**}, {@code /js/**}
   * et {@code /images/**} sans authentification.</p>
   *
   * @param registry le registre des handlers de ressources
   */
  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    registry.addResourceHandler(ROUTE_CSS)
      .addResourceLocations(STATIC_RESOURCES_PATH + "css/");

    registry.addResourceHandler(ROUTE_JS)
      .addResourceLocations(STATIC_RESOURCES_PATH + "js/");

    registry.addResourceHandler(ROUTE_IMAGES)
      .addResourceLocations(STATIC_RESOURCES_PATH + "images/");

    registry.addResourceHandler(ROUTE_WEBJARS)
      .addResourceLocations(WEBJARS_PATH);
  }

  /**
   * Résolveur de locale — français par défaut pour toute l'application.
   *
   * <p>La locale FR garantit que les messages de validation Bean Validation
   * ({@code @NotBlank}, {@code @Size}, etc.) s'affichent en français
   * dans les templates Thymeleaf.</p>
   *
   * @return le résolveur de locale basé sur la session
   */
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.FRENCH);
    return resolver;
  }

  /**
   * Source des messages i18n chargés depuis {@code messages.properties}.
   *
   * <p>Utilisé par Thymeleaf via {@code #{clé}} pour les libellés
   * et messages d'erreur de l'interface.</p>
   *
   * @return la source de messages rechargeable
   */
  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source =
      new ReloadableResourceBundleMessageSource();
    source.setBasename(MESSAGE_BASENAME);
    source.setDefaultEncoding(MESSAGE_ENCODING);
    source.setUseCodeAsDefaultMessage(true);
    return source;
  }
}
