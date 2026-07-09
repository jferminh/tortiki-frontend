package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Contrôleur de consultation publique des annonces.
 *
 * <p>Ces routes sont accessibles sans authentification — Théo doit pouvoir
 * consulter une fiche plat avant de se connecter. Le formulaire de contact
 * affiché sur cette fiche redirige vers {@code ContactController} qui, lui,
 * exige une authentification.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ListingController {

  private static final String VIEW_LISTING_DETAIL = "listing-detail";
  private static final String ATTR_LISTING = "listing";
  private static final String ATTR_CONTACT_REQUEST = "contactRequest";

  private final ListingApiClient listingApiClient;

  /**
   * Affiche la fiche publique d'une annonce.
   *
   * @param id identifiant de l'annonce
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-detail}
   */
  @GetMapping("/listings/{id}")
  public String detail(@PathVariable final Long id, final Model model) {
    log.info("Consultation de la fiche annonce {}", id);
    model.addAttribute(ATTR_LISTING, listingApiClient.findById(id));
    if (!model.containsAttribute(ATTR_CONTACT_REQUEST)) {
      model.addAttribute(ATTR_CONTACT_REQUEST, new CreateContactRequestRequest(id, null, null));
    }
    return VIEW_LISTING_DETAIL;
  }
}