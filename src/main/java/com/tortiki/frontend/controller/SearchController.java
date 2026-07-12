package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.dto.listing.ListingCardResponse;
import com.tortiki.frontend.dto.search.SearchCriteria;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Contrôleur de recherche et d'affichage des résultats d'annonces.
 *
 * <p>Consomme {@code SearchApiClient} pour interroger {@code tortiki-api}
 * selon des critères géographiques et de filtre optionnels.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

  /** Numéro de page par défaut si non précisé par le visiteur. */
  private static final int DEFAULT_PAGE = 0;

  /** Nombre de résultats par page par défaut. */
  private static final int DEFAULT_PAGE_SIZE = 12;

  /** Nom de l'attribut du modèle listant les types de cuisine. */
  private static final String ATTR_CUISINE_TYPES = "cuisineTypes";

  /** Client Feign pour les endpoints de recherche. */
  private final SearchApiClient searchApiClient;

  /**
   * Affiche le formulaire de recherche vide (sans résultat).
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code search}
   */
  @GetMapping("/search")
  public String searchForm(final Model model) {
    model.addAttribute(ATTR_CUISINE_TYPES, searchApiClient.getCuisineTypes());
    return "search";
  }

  /**
   * Recherche des annonces selon les critères fournis et affiche
   * la grille de résultats.
   *
   * <p>{@code page} et {@code size} appliquent des valeurs par défaut
   * si le visiteur ne les a pas explicitement précisés dans l'URL.</p>
   *
   * @param criteria critères de recherche liés depuis les paramètres de requête
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code search-results}
   */
  @GetMapping("/search/results")
  public String search(
      @ModelAttribute final SearchCriteria criteria,
      final Model model) {
    log.debug("Recherche avec critères : {}", criteria);
    final int page = criteria.page() != null ? criteria.page() : DEFAULT_PAGE;
    final int size = criteria.size() != null && criteria.size() > 0
        ? criteria.size() : DEFAULT_PAGE_SIZE;
    final List<ListingCardResponse> results = searchApiClient.search(
        criteria.query(),
        criteria.city(),
        criteria.postalCode(),
        criteria.cuisineTypeId(),
        page,
        size
    );
    model.addAttribute("results", results);
    model.addAttribute("criteria", criteria);
    model.addAttribute(ATTR_CUISINE_TYPES, searchApiClient.getCuisineTypes());
    return "search-results";
  }
}