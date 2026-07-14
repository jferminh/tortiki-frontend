package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.client.ReviewApiClient;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import com.tortiki.frontend.dto.listing.ListingSummaryResponse;
import java.util.List;
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
 * consulter la liste des plats disponibles et une fiche plat avant de se
 * connecter. Le formulaire de contact affiché sur la fiche redirige vers
 * {@code ContactController} qui, lui, exige une authentification. Les avis
 * affichés proviennent d'un endpoint également public —
 * {@code ReviewApiClient#findByListingId}.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ListingController {

  private static final String VIEW_LISTINGS = "listings";
  private static final String VIEW_LISTING_DETAIL = "listing-detail";
  private static final String ATTR_LISTINGS = "listings";
  private static final String ATTR_LISTING = "listing";
  private static final String ATTR_CONTACT_REQUEST = "contactRequest";
  private static final String ATTR_REVIEWS = "reviews";

  private final ListingApiClient listingApiClient;
  private final ReviewApiClient reviewApiClient;

  /**
   * Affiche la liste de toutes les annonces actives disponibles.
   *
   * <p>Distinct de {@code SearchController} : cette vue ne propose aucun
   * filtre, elle liste simplement l'ensemble des plats actifs — point
   * d'entrée le plus direct pour un visiteur qui veut simplement parcourir
   * l'offre sans critère de recherche.</p>
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listings}
   */
  @GetMapping("/listings")
  public String listAll(final Model model) {
    log.info("Consultation de la liste des annonces actives");
    final List<ListingSummaryResponse> listings = listingApiClient.findAll();
    model.addAttribute(ATTR_LISTINGS, listings);
    return VIEW_LISTINGS;
  }

  /**
   * Affiche la fiche publique d'une annonce, avec ses avis.
   *
   * @param id identifiant de l'annonce
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-detail}
   */
  @GetMapping("/listings/{id}")
  public String detail(@PathVariable final Long id, final Model model) {
    log.info("Consultation de la fiche annonce {}", id);
    model.addAttribute(ATTR_LISTING, listingApiClient.findById(id));
    model.addAttribute(ATTR_REVIEWS, reviewApiClient.findByListingId(id));
    if (!model.containsAttribute(ATTR_CONTACT_REQUEST)) {
      model.addAttribute(ATTR_CONTACT_REQUEST, new CreateContactRequestRequest(id, null, null));
    }
    return VIEW_LISTING_DETAIL;
  }
}