// src/main/java/com/tortiki/frontend/client/ReviewApiClient.java
package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.review.CreateReviewRequest;
import com.tortiki.frontend.dto.review.ReviewResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour la soumission d'évaluations.
 *
 * <p>Consomme {@code POST /api/v1/reviews} de tortiki-api.</p>
 */
@FeignClient(name = "review-api", url = "${tortiki.api.url}")
public interface ReviewApiClient {

  /**
   * Soumet une évaluation pour une annonce.
   *
   * @param request DTO avec listingId, rating, comment
   * @return évaluation persistée avec son identifiant
   */
  @PostMapping("/api/v1/reviews")
  ReviewResponse submit(@RequestBody CreateReviewRequest request);
}