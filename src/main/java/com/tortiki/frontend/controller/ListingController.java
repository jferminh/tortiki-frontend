package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.client.ReviewApiClient;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import com.tortiki.frontend.dto.listing.ListingSummaryResponse;
import com.tortiki.frontend.dto.review.ReviewResponse;
import feign.FeignException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contrôleur de consultation publique des annonces.
 *
 * <p>Ces routes sont accessibles sans authentification — Théo doit pouvoir
 * consulter la liste des plats disponibles et une fiche plat avant de se
 * connecter. Le formulaire de contact affiché sur la fiche redirige vers
 * {@code ContactController} qui, lui, exige une authentification. Les avis
 * affichés proviennent d'un endpoint également public —
 * {@code ReviewApiClient#findByListingId}, dont l'indisponibilité ne doit
 * jamais empêcher l'affichage du plat lui-même.</p>
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
   * <p>La récupération des avis est isolée dans un bloc try/catch dédié :
   * une panne du service d'évaluations ne doit jamais empêcher l'affichage
   * de l'annonce elle-même — dégradation progressive plutôt que panne totale.</p>
   *
   * @param id    identifiant de l'annonce
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code listing-detail}
   * @throws ResponseStatusException 404 si l'annonce n'existe pas
   */
  @GetMapping("/listings/{id}")
  public String detail(@PathVariable final Long id, final Model model) {
    log.info("Consultation de la fiche annonce {}", id);

    try {
      model.addAttribute(ATTR_LISTING, listingApiClient.findById(id));
    } catch (final FeignException.NotFound notFound) {
      log.warn("Annonce {} introuvable", id);
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Annonce introuvable");
    }

    model.addAttribute(ATTR_REVIEWS, fetchReviewsSafely(id));

    if (!model.containsAttribute(ATTR_CONTACT_REQUEST)) {
      model.addAttribute(ATTR_CONTACT_REQUEST, new CreateContactRequestRequest(id, null, null));
    }
    return VIEW_LISTING_DETAIL;
  }

  /**
   * Récupère les avis d'une annonce sans jamais faire échouer la page.
   *
   * @param listingId identifiant de l'annonce
   * @return liste des avis, ou liste vide si le service est indisponible
   */
  private List<ReviewResponse> fetchReviewsSafely(final Long listingId) {
    try {
      return reviewApiClient.findByListingId(listingId);
    } catch (final FeignException feignException) {
      log.warn("Service avis indisponible pour l'annonce {} — page affichee sans avis",
          listingId);
      return Collections.emptyList();
    }
  }
}