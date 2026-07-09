package com.tortiki.frontend.dto.review;

import java.time.LocalDateTime;

/**
 * Réponse après création d'une évaluation.
 *
 * @param id identifiant de l'évaluation créée
 * @param listingId identifiant de l'annonce évaluée
 * @param reviewerFirstName prénom de l'auteur
 * @param rating note attribuée (1-5)
 * @param comment commentaire
 * @param createdAt date de création
 */
public record ReviewResponse(
    Long id,
    Long listingId,
    String reviewerFirstName,
    Integer rating,
    String comment,
    LocalDateTime createdAt
) {
}