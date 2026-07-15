package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.client.ReviewApiClient;
import com.tortiki.frontend.dto.review.CreateReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur de soumission des évaluations acheteur.
 *
 * <p>Réservé implicitement aux utilisateurs {@code ROLE_BUYER} : la page
 * n'est accessible que via le bouton "Laisser un avis" de
 * {@code buyer-requests.html}, lui-même conditionné à un statut
 * {@code CONFIRMED}. La vérification stricte du rôle et de l'éligibilité
 * (demande bien confirmée, absence d'avis déjà soumis) reste imposée en
 * profondeur par {@code tortiki-api}, jamais recalculée côté frontend.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReviewController {

  private static final String VIEW_SUBMIT_REVIEW = "submit-review";
  private static final String ATTR_LISTING = "listing";
  private static final String ATTR_REVIEW_REQUEST = "reviewRequest";
  private static final String ATTR_SUCCESS = "success";
  private static final String REDIRECT_BUYER_REQUESTS = "redirect:/buyer/requests";

  private final ListingApiClient listingApiClient;
  private final ReviewApiClient reviewApiClient;

  /**
   * Affiche le formulaire de soumission d'un avis pour une annonce.
   *
   * @param listingId identifiant de l'annonce à évaluer
   * @param model     modèle Thymeleaf
   * @return nom de la vue {@code submit-review}
   */
  @GetMapping("/reviews/new")
  public String newReviewForm(@RequestParam final Long listingId, final Model model) {
    log.debug("Affichage du formulaire d'avis pour l'annonce {}", listingId);

    model.addAttribute(ATTR_LISTING, listingApiClient.findById(listingId));

    if (!model.containsAttribute(ATTR_REVIEW_REQUEST)) {
      model.addAttribute(ATTR_REVIEW_REQUEST, new CreateReviewRequest(listingId, null, null));
    }
    return VIEW_SUBMIT_REVIEW;
  }

  /**
   * Soumet un avis pour une annonce.
   *
   * @param request           formulaire d'avis, validé par Bean Validation
   * @param bindingResult     résultat de validation, réaffiché sur erreur
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers l'historique des demandes en cas de succès,
   *     ou réaffichage du formulaire en cas d'erreur de validation
   */
  @PostMapping("/reviews")
  public String submit(
      @Valid @ModelAttribute(ATTR_REVIEW_REQUEST) final CreateReviewRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      log.warn("Erreurs de validation à la soumission d'un avis pour l'annonce {}",
          request.listingId());
      redirectAttributes.addFlashAttribute(
          "org.springframework.validation.BindingResult." + ATTR_REVIEW_REQUEST, bindingResult);
      redirectAttributes.addFlashAttribute(ATTR_REVIEW_REQUEST, request);
      return "redirect:/reviews/new?listingId=" + request.listingId();
    }

    reviewApiClient.submit(request);
    log.info("Avis soumis pour l'annonce {}", request.listingId());
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Merci, votre avis a bien été publié.");
    return REDIRECT_BUYER_REQUESTS;
  }
}