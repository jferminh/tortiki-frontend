package com.tortiki.frontend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Commande de soumission d'une évaluation.
 *
 * @param listingId identifiant de l'annonce évaluée
 * @param rating    note de 1 à 5
 * @param comment   commentaire libre, peut être {@code null}
 */
public record CreateReviewRequest(
    @NotNull(message = "L'identifiant de l'annonce est obligatoire")
    Long listingId,

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    Integer rating,

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String comment
) {
}