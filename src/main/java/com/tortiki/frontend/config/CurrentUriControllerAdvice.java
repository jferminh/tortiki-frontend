package com.tortiki.frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose l'URI de la requête courante à tous les modèles Thymeleaf.
 *
 * <p>Depuis Thymeleaf 3.1, les objets implicites {@code #request},
 * {@code #session} et {@code #servletContext} sont désactivés par défaut
 * pour des raisons de sécurité. Cette classe injecte explicitement
 * l'attribut {@code currentUri}, seule alternative recommandée pour
 * accéder à l'URI courante dans un fragment partagé comme la navbar.</p>
 */
@ControllerAdvice
public class CurrentUriControllerAdvice {

  /**
   * Ajoute l'URI de la requête courante au modèle de chaque vue.
   *
   * @param request la requête HTTP courante, injectée par Spring MVC
   * @return l'URI de la requête, sans query string
   */
  @ModelAttribute("currentUri")
  public String currentUri(final HttpServletRequest request) {
    return request.getRequestURI();
  }
}