package com.tortiki.frontend.dto.contact;

/**
 * Commande de mise à jour du statut d'une demande.
 *
 * @param newStatus nouveau statut souhaité par le vendeur
 */
public record UpdateContactRequestStatusRequest(
    ContactRequestStatus newStatus
) {
}