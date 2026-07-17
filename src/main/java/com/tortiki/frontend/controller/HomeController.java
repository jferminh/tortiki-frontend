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
 * <p>Charge les types de cuisine actifs et les villes distinctes ayant
 * au moins une annonce active, depuis l'API. Réutilise
 * {@link SearchApiClient}, déjà exploité par la recherche et le panel
 * d'administration.</p>
 *
 * <p>Applique une dégradation gracieuse (circuit breaker « soft ») : la
 * page d'accueil étant le point d'entrée public du site, une panne de
 * {@code tortiki-api} ne doit jamais empêcher son affichage. En cas
 * d'échec de l'appel distant, les sections concernées sont simplement
 * vidées plutôt que de propager une erreur 500 au visiteur.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private static final String VIEW_HOME = "home";
  private static final String ATTR_CUISINE_TYPES = "cuisineTypes";
  private static final String ATTR_ACTIVE_CITIES = "activeCities";

  /** Client Feign pour les endpoints de recherche et de référentiel. */
  private final SearchApiClient searchApiClient;

  /**
   * Affiche la page d'accueil avec les types de cuisine et les villes
   * actives disponibles.
   *
   * <p>Si {@code tortiki-api} est indisponible, la page reste accessible
   * avec des listes vides plutôt que de faire échouer le rendu de la
   * page entière.</p>
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code home}
   */
  @GetMapping("/")
  public String home(final Model model) {
    log.debug("Chargement de la page d'accueil");
    model.addAttribute(ATTR_CUISINE_TYPES, fetchCuisineTypesSafely());
    model.addAttribute(ATTR_ACTIVE_CITIES, fetchActiveCitiesSafely());
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

  /**
   * Récupère les villes distinctes actives en absorbant toute panne distante.
   *
   * <p>Une panne sur cet appel ne doit pas empêcher l'affichage de la page
   * ni du champ de recherche — seule l'autocomplétion est désactivée,
   * le visiteur peut toujours saisir une ville librement.</p>
   *
   * @return liste des villes actives, ou liste vide si {@code tortiki-api}
   *     est indisponible ou répond en erreur
   */
  private List<String> fetchActiveCitiesSafely() {
    try {
      return searchApiClient.getActiveCities();
    } catch (final FeignException ex) {
      log.warn("Liste des villes actives indisponible, champ de recherche "
          + "affiché sans autocomplétion : {}", ex.getMessage());
      return List.of();
    }
  }
}