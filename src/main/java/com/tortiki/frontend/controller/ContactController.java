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
 * authentification (règle {@code anyRequest().authenticated()} dans
 * {@code SecurityConfig}). Le contrôle du rôle exact ({@code ROLE_BUYER})
 * n'est pas effectué ici — il est délégué à {@code tortiki-api}, qui
 * refuse la soumission en 403 si l'appelant n'a pas ce rôle.</p>
 */
@Slf4j
@Controller
@RequestMapping("/contact-requests")
@RequiredArgsConstructor
public class ContactController {

  /**
   * Clé conventionnelle Spring pour exposer un {@link BindingResult} en
   * attribut flash — doit être préfixée exactement ainsi pour que
   * Thymeleaf résolve {@code th:errors} sur {@code contactRequest} après
   * redirection.
   */
  private static final String FLASH_BINDING_RESULT_PREFIX =
      "org.springframework.validation.BindingResult.";
  private static final String ATTR_CONTACT_REQUEST = "contactRequest";
  private static final String ATTR_SUCCESS = "success";

  private final ContactApiClient contactApiClient;

  /**
   * Soumet une demande de contact pour une annonce.
   *
   * @param request formulaire de contact, validé par Bean Validation
   * @param bindingResult résultat de validation, réaffiché sur erreur
   * @param redirectAttributes messages flash de confirmation ou d'erreur
   * @return redirection vers la fiche plat, avec succès ou erreurs de validation
   */
  @PostMapping
  public String submit(
      @Valid @ModelAttribute(ATTR_CONTACT_REQUEST) final CreateContactRequestRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      log.warn("Erreurs de validation à la soumission d'une demande de contact pour l'annonce {}",
          request.listingId());
      redirectAttributes.addFlashAttribute(
          FLASH_BINDING_RESULT_PREFIX + ATTR_CONTACT_REQUEST, bindingResult);
      redirectAttributes.addFlashAttribute(ATTR_CONTACT_REQUEST, request);
      return buildRedirectToListing(request.listingId());
    }
    contactApiClient.submit(request);
    log.info("Demande de contact soumise pour l'annonce {}", request.listingId());
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Votre demande a bien été envoyée.");
    return buildRedirectToListing(request.listingId());
  }

  /**
   * Construit la redirection vers la fiche plat correspondante.
   *
   * @param listingId identifiant de l'annonce
   * @return chemin de redirection {@code redirect:/listings/{id}}
   */
  private String buildRedirectToListing(final Long listingId) {
    return "redirect:/listings/" + listingId;
  }
}