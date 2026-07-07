package com.tortiki.frontend.dto.admin;

import java.math.BigDecimal;

/**
 * Résumé d'une annonce affiché dans le panel d'administration.
 *
 * @param id          identifiant de l'annonce
 * @param title       titre du plat
 * @param sellerEmail email du vendeur (usage interne admin uniquement)
 * @param city        ville de retrait
 * @param price       prix unitaire
 * @param status      statut actuel de l'annonce
 * @param photoUrl    URL de la photo (peut être null)
 */
public record AdminListingSummaryResponse(
    Long id,
    String title,
    String sellerEmail,
    String city,
    BigDecimal price,
    String status,
    String photoUrl) {
}