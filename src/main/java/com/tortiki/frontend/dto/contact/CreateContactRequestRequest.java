package com.tortiki.frontend.dto.contact;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Commande de soumission d'une demande de contact.
 *
 * @param listingId identifiant de l'annonce cible
 * @param message message libre de l'acheteur
 * @param portions nombre de portions souhaitées
 */
public record CreateContactRequestRequest(
    @NotNull(message = "L'annonce concernée est obligatoire")
    Long listingId,

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 500, message = "Le message ne doit pas dépasser 500 caractères")
    String message,

    @NotNull(message = "Le nombre de portions est obligatoire")
    @Min(value = 1, message = "Le nombre de portions doit être au moins 1")
    Integer portions
) {
}