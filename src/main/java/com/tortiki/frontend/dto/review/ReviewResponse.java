package com.tortiki.frontend.dto.review;

import java.time.LocalDateTime;

/**
 * Représentation d'une évaluation reçue depuis tortiki-api.
 *
 * <p>Miroir exact du contrat {@code ReviewResponse} côté backend —
 * expose uniquement le prénom de l'acheteur, jamais son email
 * (minimisation RGPD déjà actée côté API).</p>
 *
 * @param id                identifiant de l'évaluation
 * @param listingId         identifiant de l'annonce évaluée
 * @param reviewerFirstName prénom de l'acheteur auteur de l'évaluation
 * @param rating            note attribuée, comprise entre 1 et 5
 * @param comment           commentaire libre, éventuellement absent
 * @param createdAt         date et heure de création de l'évaluation
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