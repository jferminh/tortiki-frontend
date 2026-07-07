package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.AdminApiClient;
import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.dto.admin.UpdateListingStatusRequest;
import com.tortiki.frontend.dto.listing.CreateCuisineTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur MVC du panel d'administration.
 *
 * <p>Réservé au rôle {@code ROLE_ADMIN} (contrôle d'accès délégué à
 * {@code SecurityConfig} et vérifié en profondeur par tortiki-api).
 * Couvre deux périmètres fonctionnels distincts : modération des annonces
 * et gestion du référentiel des types de cuisine.</p>
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private static final String VIEW_DASHBOARD = "admin-dashboard";
  private static final String VIEW_LISTINGS = "admin-listings";
  private static final String VIEW_CUISINE_TYPES = "admin-cuisine-types";
  private static final String ATTR_LISTINGS = "listings";
  private static final String ATTR_CUISINE_TYPES = "cuisineTypes";
  private static final String ATTR_CREATE_CUISINE_TYPE_REQUEST = "createCuisineTypeRequest";
  private static final String ATTR_SUCCESS = "success";
  private static final String REDIRECT_ADMIN_LISTINGS = "redirect:/admin/listings";
  private static final String REDIRECT_ADMIN_CUISINE_TYPES = "redirect:/admin/cuisine-types";

  private final AdminApiClient adminApiClient;
  private final SearchApiClient searchApiClient;

  /**
   * Affiche le tableau de bord administrateur avec des indicateurs globaux.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code admin-dashboard}
   */
  @GetMapping
  public String dashboard(final Model model) {
    log.info("Chargement du dashboard admin");
    var listings = adminApiClient.getAllListings();
    var cuisineTypes = searchApiClient.getCuisineTypes();
    model.addAttribute(ATTR_LISTINGS, listings);
    model.addAttribute(ATTR_CUISINE_TYPES, cuisineTypes);
    return VIEW_DASHBOARD;
  }

  /**
   * Affiche la liste complète des annonces pour modération.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code admin-listings}
   */
  @GetMapping("/listings")
  public String listings(final Model model) {
    log.info("Chargement de la modération des annonces");
    model.addAttribute(ATTR_LISTINGS, adminApiClient.getAllListings());
    return VIEW_LISTINGS;
  }

  /**
   * Modifie le statut d'une annonce (activation/désactivation).
   *
   * @param id                 identifiant de l'annonce
   * @param newStatus          nouveau statut souhaité
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers {@code /admin/listings}
   */
  @PostMapping("/listings/{id}/status")
  public String updateListingStatus(
      @PathVariable final Long id,
      final String newStatus,
      final RedirectAttributes redirectAttributes) {
    adminApiClient.updateListingStatus(id, new UpdateListingStatusRequest(newStatus));
    log.info("Statut de l'annonce {} changé en {}", id, newStatus);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Statut de l'annonce mis à jour.");
    return REDIRECT_ADMIN_LISTINGS;
  }

  /**
   * Affiche le référentiel des types de cuisine avec le formulaire de création.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code admin-cuisine-types}
   */
  @GetMapping("/cuisine-types")
  public String cuisineTypes(final Model model) {
    log.info("Chargement du référentiel des types de cuisine");
    if (!model.containsAttribute(ATTR_CREATE_CUISINE_TYPE_REQUEST)) {
      model.addAttribute(ATTR_CREATE_CUISINE_TYPE_REQUEST,
          new CreateCuisineTypeRequest("", ""));
    }
    model.addAttribute(ATTR_CUISINE_TYPES, searchApiClient.getCuisineTypes());
    return VIEW_CUISINE_TYPES;
  }

  /**
   * Crée un nouveau type de cuisine dans le référentiel.
   *
   * @param request            formulaire de création, validé par Bean Validation
   * @param bindingResult      résultat de validation, réaffiché sur erreur
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers {@code /admin/cuisine-types}
   */
  @PostMapping("/cuisine-types")
  public String createCuisineType(
      @Valid @ModelAttribute(ATTR_CREATE_CUISINE_TYPE_REQUEST)
      final CreateCuisineTypeRequest request,
      final BindingResult bindingResult,
      final Model model,
      final RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      log.warn("Erreurs de validation à la création d'un type de cuisine");
      model.addAttribute(ATTR_CUISINE_TYPES, searchApiClient.getCuisineTypes());
      return VIEW_CUISINE_TYPES;
    }
    adminApiClient.createCuisineType(request);
    log.info("Type de cuisine créé : {}", request.name());
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Type de cuisine créé avec succès.");
    return REDIRECT_ADMIN_CUISINE_TYPES;
  }

  /**
   * Supprime un type de cuisine du référentiel.
   *
   * @param id                 identifiant du type de cuisine à supprimer
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers {@code /admin/cuisine-types}
   */
  @PostMapping("/cuisine-types/{id}/delete")
  public String deleteCuisineType(
      @PathVariable final Long id,
      final RedirectAttributes redirectAttributes) {
    adminApiClient.deleteCuisineType(id);
    log.info("Type de cuisine {} supprimé", id);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Type de cuisine supprimé.");
    return REDIRECT_ADMIN_CUISINE_TYPES;
  }
}