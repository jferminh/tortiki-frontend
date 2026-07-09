package com.tortiki.frontend.dto.contact;

/**
 * Résumé d'une demande de contact reçue par un vendeur.
 *
 * @param id             identifiant de la demande
 * @param listingId      identifiant de l'annonce concernée
 * @param listingTitle   titre de l'annonce
 * @param buyerFirstName prénom de l'acheteur (minimisation RGPD, pas d'email)
 * @param message        message de l'acheteur
 * @param portions       nombre de portions souhaitées
 * @param status         statut actuel de la demande
 */
public record ContactRequestSummaryResponse(
    Long id,
    Long listingId,
    String listingTitle,
    String buyerFirstName,
    String message,
    Integer portions,
    ContactRequestStatus status
) {
}