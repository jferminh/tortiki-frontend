package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.dto.listing.AllergenResponse;
import com.tortiki.frontend.dto.listing.CreateListingRequest;
import feign.FeignException;
import jakarta.validation.Valid;
import java.util.List;
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

  private static final String VIEW_LISTING_FORM = "listing-form";
  private static final String VIEW_SELLER_LISTINGS = "seller-listings";
  private static final String REDIRECT_SELLER_LISTINGS = "redirect:/seller/listings";
  /**
   * Nom de l'attribut de modèle/flash portant le message de succès.
   */
  private static final String ATTR_SUCCESS = "success";
  private static final String ATTR_CUISINE_TYPES = "cuisineTypes";
  private static final String ATTR_ALLERGENS = "allergens";
  private static final String ATTR_IS_EDIT = "isEdit";
  private static final String ATTR_FORM_ACTION = "formAction";
  private static final String ROUTE_SELLER_LISTINGS_NEW = "/seller/listings/new";

  private final ListingApiClient listingApiClient;

  /**
   * Liste les annonces publiées par le vendeur connecté, tous statuts confondus.
   *
   * <p>L'identité du vendeur n'est plus extraite du {@code Principal} local :
   * elle est résolue côté {@code tortiki-api} depuis le cookie de session
   * relayé par Feign (voir {@code FeignConfig#sessionCookieInterceptor}
   * et {@code SellerListingController} de tortiki-api).</p>
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code seller-listings}
   */
  @GetMapping
  public String myListings(final Model model) {
    model.addAttribute("listings", listingApiClient.getMyListings());
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
        new CreateListingRequest(null, null, null, null, null, null, null, null));
    populateFormReferenceData(model);
    model.addAttribute(ATTR_IS_EDIT, false);
    model.addAttribute(ATTR_FORM_ACTION, ROUTE_SELLER_LISTINGS_NEW);
    return VIEW_LISTING_FORM;
  }

  /**
   * Traite la soumission du formulaire de création.
   *
   * @param request            données saisies, validées via Bean Validation
   * @param bindingResult      résultat de la validation
   * @param photo              fichier photo optionnel
   * @param model              modèle Thymeleaf, nécessaire pour recharger listes en cas d'erreur
   * @param redirectAttributes messages flash
   * @return redirection vers la liste des annonces en cas de succès
   */
  @PostMapping("/new")
  public String createListing(
      @Valid @ModelAttribute("listingRequest") final CreateListingRequest request,
      final BindingResult bindingResult,
      @RequestParam(required = false) final MultipartFile photo,
      final Model model,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateFormReferenceData(model);
      model.addAttribute(ATTR_IS_EDIT, false);
      model.addAttribute(ATTR_FORM_ACTION, ROUTE_SELLER_LISTINGS_NEW);
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
   * @param id    identifiant de l'annonce
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-form}
   */
  @GetMapping("/{id}/edit")
  public String editListingForm(@PathVariable final Long id, final Model model) {
    var listing = listingApiClient.getById(id);
    model.addAttribute("listingRequest", new CreateListingRequest(
        listing.title(),
        listing.description(),
        listing.price(),
        listing.portions(),
        listing.pickupAddress(),
        listing.pickupDatetime(),
        listing.cuisineTypeId(),
        listing.allergenIds()));
    populateFormReferenceData(model);
    model.addAttribute("listingId", id);
    model.addAttribute(ATTR_IS_EDIT, true);
    model.addAttribute(ATTR_FORM_ACTION, editFormAction(id));
    return VIEW_LISTING_FORM;
  }

  /**
   * Traite la soumission du formulaire d'édition.
   *
   * @param id                 identifiant de l'annonce
   * @param request            données mises à jour
   * @param bindingResult      résultat de la validation
   * @param model              modèle Thymeleaf, nécessaire pour recharger listes en cas d'erreur
   * @param redirectAttributes messages flash
   * @return redirection vers la liste des annonces en cas de succès
   */
  @PostMapping("/{id}/edit")
  public String updateListing(
      @PathVariable final Long id,
      @Valid @ModelAttribute("listingRequest") final CreateListingRequest request,
      final BindingResult bindingResult,
      final Model model,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateFormReferenceData(model);
      model.addAttribute("listingId", id);
      model.addAttribute(ATTR_IS_EDIT, true);
      model.addAttribute(ATTR_FORM_ACTION, editFormAction(id));
      return VIEW_LISTING_FORM;
    }
    listingApiClient.update(id, request);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Annonce mise à jour avec succès.");
    return REDIRECT_SELLER_LISTINGS;
  }

  /**
   * Désactive une annonce (suppression logique, statut {@code INACTIVE}).
   *
   * <p>Exposée en {@code POST} plutôt qu'en vrai {@code DELETE} HTTP :
   * un formulaire HTML natif ne supporte que {@code GET}/{@code POST}, et
   * aucun {@code HiddenHttpMethodFilter} n'est configuré dans ce projet.
   * Ce choix évite d'introduire un filtre global supplémentaire pour un
   * seul cas d'usage — cohérent avec {@link #createListing} et
   * {@link #updateListing}, également exposées en {@code POST}.</p>
   *
   * <p>Aucune vérification d'autorisation n'est effectuée ici : si Sofia
   * tentait de désactiver l'annonce d'un autre vendeur, {@code tortiki-api}
   * répondrait par une {@code UnauthorizedActionException} (403), déjà
   * gérée par {@code GlobalExceptionHandler}.</p>
   *
   * @param id                 identifiant de l'annonce à désactiver
   * @param redirectAttributes message flash affiché après redirection
   * @return redirection vers la liste des annonces
   */
  @PostMapping("/{id}/delete")
  public String deleteListing(
      @PathVariable final Long id,
      final RedirectAttributes redirectAttributes) {
    listingApiClient.delete(id);
    log.debug("Annonce id={} désactivée par le vendeur connecté", id);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Annonce désactivée avec succès.");
    return REDIRECT_SELLER_LISTINGS;
  }

  /**
   * Charge les données de référence communes au formulaire d'annonce.
   * L'appel aux allergènes est isolé, car l'endpoint backend est en cours
   * de stabilisation (voir Issue tortiki-api correspondante).
   *
   * @param model modèle Thymeleaf à enrichir
   */
  private void populateFormReferenceData(final Model model) {
    model.addAttribute(ATTR_CUISINE_TYPES, listingApiClient.getCuisineTypes());
    model.addAttribute(ATTR_ALLERGENS, safeGetAllergens());
  }

  /**
   * Récupère la liste des allergènes en tolérant une indisponibilité
   * temporaire de l'API (endpoint backend en cours de stabilisation).
   *
   * @return liste des allergènes, vide en cas d'erreur serveur
   */
  private List<AllergenResponse> safeGetAllergens() {
    try {
      return listingApiClient.getAllergens();
    } catch (FeignException.InternalServerError feignException) {
      log.warn(
          "Endpoint /api/v1/allergens indisponible côté API : {}", feignException.getMessage()
      );
      return List.of();
    }
  }

  /**
   * Construit l'URL de soumission du formulaire d'édition.
   *
   * @param id identifiant de l'annonce
   * @return chemin relatif du formulaire d'édition
   */
  private String editFormAction(final Long id) {
    return "/seller/listings/" + id + "/edit";
  }
}