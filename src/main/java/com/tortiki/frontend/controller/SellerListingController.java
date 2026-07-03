package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.dto.listing.CreateListingRequest;
import jakarta.validation.Valid;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur de gestion des annonces vendeur (Sofia).
 *
 * <p>Toutes les routes de ce contrôleur sont protégées par
 * {@code SecurityConfig} sous {@code /seller/**} — accès réservé
 * aux utilisateurs authentifiés, le rôle ROLE_SELLER étant vérifié
 * côté API.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/seller/listings")
public class SellerListingController {

  /** Nom de la vue Thymeleaf du formulaire d'annonce. */
  private static final String VIEW_LISTING_FORM = "listing-form";

  /** Nom de la vue Thymeleaf de la liste des annonces. */
  private static final String VIEW_SELLER_LISTINGS = "seller-listings";

  /** Route de redirection vers la liste des annonces après création/édition. */
  private static final String REDIRECT_SELLER_LISTINGS = "redirect:/seller/listings";

  /** Nom de l'attribut de modèle/flash portant le message de succès. */
  private static final String ATTR_SUCCESS = "success";

  /** Client Feign pour la gestion des annonces. */
  private final ListingApiClient listingApiClient;

  /**
   * Liste les annonces publiées par le vendeur connecté.
   *
   * @param model modèle Thymeleaf
   * @param principal utilisateur authentifié
   * @return nom de la vue {@code seller-listings}
   */
  @GetMapping
  public String myListings(final Model model, final Principal principal) {
    model.addAttribute("listings", listingApiClient.getMyListings(principal.getName()));
    if (!model.containsAttribute(ATTR_SUCCESS)) {
      model.addAttribute(ATTR_SUCCESS, null);
    }
    return VIEW_SELLER_LISTINGS;
  }

  /**
   * Affiche le formulaire de création d'une nouvelle annonce.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-form}
   */
  @GetMapping("/new")
  public String newListingForm(final Model model) {
    model.addAttribute("listingRequest",
        new CreateListingRequest("", "", null, null, null, null, ""));
    model.addAttribute("cuisineTypes", listingApiClient.getCuisineTypes());
    model.addAttribute("isEdit", false);
    model.addAttribute("formAction", "/seller/listings/new");
    return VIEW_LISTING_FORM;
  }

  /**
   * Traite la soumission du formulaire de création.
   *
   * @param request données saisies, validées via Bean Validation
   * @param bindingResult résultat de la validation
   * @param photo fichier photo optionnel
   * @param redirectAttributes messages flash
   * @return redirection vers la liste des annonces en cas de succès
   */
  @PostMapping("/new")
  public String createListing(
      @Valid @ModelAttribute("listingRequest") final CreateListingRequest request,
      final BindingResult bindingResult,
      @RequestParam(required = false) final MultipartFile photo,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return VIEW_LISTING_FORM;
    }
    var created = listingApiClient.create(request);
    if (photo != null && !photo.isEmpty()) {
      listingApiClient.uploadPhoto(created.id(), photo);
    }
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Annonce publiée avec succès.");
    return REDIRECT_SELLER_LISTINGS;
  }

  /**
   * Affiche le formulaire d'édition pré-rempli d'une annonce existante.
   *
   * @param id identifiant de l'annonce
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-form}
   */
  @GetMapping("/{id}/edit")
  public String editListingForm(@PathVariable final Long id, final Model model) {
    var listing = listingApiClient.getById(id);
    model.addAttribute("listingRequest", new CreateListingRequest(
        listing.title(), listing.description(), listing.price(),
        listing.portionsAvailable(), listing.cuisineTypeId(),
        listing.allergenIds(), listing.city()));
    model.addAttribute("listingId", id);
    model.addAttribute("cuisineTypes", listingApiClient.getCuisineTypes());
    model.addAttribute("isEdit", true);
    model.addAttribute("formAction", "/seller/listings/" + id + "/edit");
    return VIEW_LISTING_FORM;
  }

  /**
   * Traite la soumission du formulaire d'édition.
   *
   * @param id identifiant de l'annonce
   * @param request données mises à jour
   * @param bindingResult résultat de la validation
   * @param redirectAttributes messages flash
   * @return redirection vers la liste des annonces en cas de succès
   */
  @PostMapping("/{id}/edit")
  public String updateListing(
      @PathVariable final Long id,
      @Valid @ModelAttribute("listingRequest") final CreateListingRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return VIEW_LISTING_FORM;
    }
    listingApiClient.update(id, request);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Annonce mise à jour avec succès.");
    return REDIRECT_SELLER_LISTINGS;
  }
}