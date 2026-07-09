package com.tortiki.frontend.dto.review;

/**
 * Commande de soumission d'une évaluation.
 *
 * @param listingId identifiant de l'annonce évaluée
 * @param rating note de 1 à 5
 * @param comment commentaire libre (peut être null)
 */
public record CreateReviewRequest(
    Long listingId,
    Integer rating,
    String comment
) {
}