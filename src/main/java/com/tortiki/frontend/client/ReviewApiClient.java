// client/ReviewApiClient.java — fichier complet mis à jour

package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.review.CreateReviewRequest;
import com.tortiki.frontend.dto.review.ReviewResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client Feign pour la consultation et la soumission d'évaluations.
 *
 * <p>Consomme {@code GET /api/v1/reviews} et {@code POST /api/v1/reviews}
 * de tortiki-api. La lecture est publique — aucune session requise,
 * contrairement à la soumission qui exige le rôle {@code BUYER} côté API.</p>
 */
@FeignClient(name = "review-api", url = "${tortiki.api.url}")
public interface ReviewApiClient {

  /**
   * Récupère les évaluations d'une annonce.
   *
   * <p>Endpoint public — appelable sans utilisateur authentifié,
   * utilisé pour afficher la section avis de la fiche plat.</p>
   *
   * @param listingId identifiant de l'annonce évaluée
   * @return liste des évaluations, vide si aucune n'existe encore
   */
  @GetMapping("/api/v1/reviews")
  List<ReviewResponse> findByListingId(@RequestParam("listingId") Long listingId);

  /**
   * Soumet une évaluation pour une annonce.
   *
   * @param request DTO avec listingId, rating, comment
   * @return évaluation persistée avec son identifiant
   */
  @PostMapping("/api/v1/reviews")
  ReviewResponse submit(@RequestBody CreateReviewRequest request);
}