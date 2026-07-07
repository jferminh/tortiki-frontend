package com.tortiki.frontend.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Commande de changement de statut d'une annonce par un administrateur.
 *
 * @param newStatus nouveau statut souhaité (ex : INACTIVE, ACTIVE)
 */
public record UpdateListingStatusRequest(
    @NotBlank(message = "Le statut est obligatoire")
    String newStatus) {
}