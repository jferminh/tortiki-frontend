package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de la page d'accueil Tortiki.
 *
 * <p>Charge les types de cuisine actifs depuis l'API
 * pour alimenter la section de navigation par cuisine.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  /** Client Feign pour les endpoints de recherche et de référentiel. */
  private final SearchApiClient searchApiClient;

  /**
   * Affiche la page d'accueil avec les types de cuisine disponibles.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code home}
   */
  @GetMapping("/")
  public String home(final Model model) {
    log.debug("Chargement de la page d'accueil");
    List<CuisineTypeResponse> cuisineTypes = searchApiClient.getCuisineTypes();
    model.addAttribute("cuisineTypes", cuisineTypes);
    return "home";
  }
}