package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ContactApiClient;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur de soumission des demandes de contact.
 *
 * <p>Contrairement à {@code ListingController}, cette route exige une
 * authentification — seul un utilisateur connecté (rôle {@code ROLE_BUYER})
 * peut soumettre une demande de contact, vérifié en profondeur par
 * {@code tortiki-api}.</p>
 */
@Slf4j
@Controller
@RequestMapping("/contact-requests")
@RequiredArgsConstructor
public class ContactController {

  private static final String VIEW_LISTING_DETAIL = "listing-detail";
  private static final String ATTR_SUCCESS = "success";

  private final ContactApiClient contactApiClient;

  /**
   * Soumet une demande de contact pour une annonce.
   *
   * @param request formulaire de contact, validé par Bean Validation
   * @param bindingResult résultat de validation, réaffiché sur erreur
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers la fiche plat en cas de succès, ou réaffichage
   *     du formulaire en cas d'erreur de validation
   */
  @PostMapping
  public String submit(
      @Valid @ModelAttribute("contactRequest") final CreateContactRequestRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      log.warn("Erreurs de validation à la soumission d'une demande de contact");
      redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult."
          + "contactRequest", bindingResult);
      redirectAttributes.addFlashAttribute("contactRequest", request);
      return "redirect:/listings/" + request.listingId();
    }
    contactApiClient.submit(request);
    log.info("Demande de contact soumise pour l'annonce {}", request.listingId());
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Votre demande a bien été envoyée.");
    return "redirect:/listings/" + request.listingId();
  }
}