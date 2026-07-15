package com.tortiki.frontend.dto.contact;

import java.time.LocalDateTime;

/**
 * Résumé d'une demande de contact soumise par l'acheteur — historique personnel.
 *
 * <p>Distinct de {@link ContactRequestSummaryResponse} (vue vendeur) : expose
 * le prénom du vendeur contacté plutôt que celui de l'acheteur, ainsi que
 * le visuel de l'annonce (confort d'affichage côté Théo).</p>
 *
 * @param id              identifiant de la demande
 * @param listingId       identifiant de l'annonce concernée
 * @param listingTitle    titre de l'annonce
 * @param listingPhotoUrl URL de la photo de l'annonce, {@code null} si absente
 * @param sellerFirstName prénom du vendeur contacté (minimisation RGPD, pas d'email)
 * @param message         message laissé par l'acheteur
 * @param portions        nombre de portions souhaitées
 * @param status          statut actuel de la demande
 * @param createdAt       date de soumission
 */
public record ContactRequestBuyerSummaryResponse(
    Long id,
    Long listingId,
    String listingTitle,
    String listingPhotoUrl,
    String sellerFirstName,
    String message,
    Integer portions,
    ContactRequestStatus status,
    LocalDateTime createdAt
) {
}