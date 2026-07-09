package com.tortiki.frontend.dto.contact;

/**
 * Commande de soumission d'une demande de contact.
 *
 * @param listingId identifiant de l'annonce cible
 * @param message message libre de l'acheteur
 * @param portions nombre de portions souhaitées
 */
public record CreateContactRequestRequest(
    Long listingId,
    String message,
    Integer portions
) {
}