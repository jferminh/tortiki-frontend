package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import feign.FeignException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de la page d'accueil Tortiki.
 *
 * <p>Charge les types de cuisine actifs depuis l'API pour alimenter
 * la section de navigation par cuisine. Réutilise {@link SearchApiClient},
 * déjà exploité par la recherche et le panel d'administration.</p>
 *
 * <p>Applique une dégradation gracieuse (circuit breaker « soft ») : la
 * page d'accueil étant le point d'entrée public du site, une panne de
 * {@code tortiki-api} ne doit jamais empêcher son affichage. En cas
 * d'échec de l'appel distant, la section de navigation par cuisine est
 * simplement vidée plutôt que de propager une erreur 500 au visiteur.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private static final String VIEW_HOME = "home";
  private static final String ATTR_CUISINE_TYPES = "cuisineTypes";

  /** Client Feign pour les endpoints de recherche et de référentiel. */
  private final SearchApiClient searchApiClient;

  /**
   * Affiche la page d'accueil avec les types de cuisine disponibles.
   *
   * <p>Si {@code tortiki-api} est indisponible, la page reste accessible
   * avec une liste de types de cuisine vide plutôt que de faire échouer
   * le rendu de la page entière.</p>
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code home}
   */
  @GetMapping("/")
  public String home(final Model model) {
    log.debug("Chargement de la page d'accueil");
    model.addAttribute(ATTR_CUISINE_TYPES, fetchCuisineTypesSafely());
    return VIEW_HOME;
  }

  /**
   * Récupère les types de cuisine actifs en absorbant toute panne distante.
   *
   * @return liste des types de cuisine, ou liste vide si {@code tortiki-api}
   *     est indisponible ou répond en erreur
   */
  private List<CuisineTypeResponse> fetchCuisineTypesSafely() {
    try {
      return searchApiClient.getCuisineTypes();
    } catch (final FeignException ex) {
      log.warn("Référentiel des types de cuisine indisponible, page d'accueil "
          + "affichée sans navigation par cuisine : {}", ex.getMessage());
      return List.of();
    }
  }
}