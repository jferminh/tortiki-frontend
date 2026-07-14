package com.tortiki.frontend.dto.listing;

import java.math.BigDecimal;

/**
 * DTO de réponse représentant une annonce active en format carte,
 * pour la vue liste globale des plats disponibles.
 *
 * <p>Record immuable Java 21 — désérialisé depuis la réponse JSON
 * de {@code GET /api/v1/listings} ({@code ListingResponse} côté
 * tortiki-api).</p>
 *
 * <p>Ce DTO est volontairement distinct de {@code ListingCardResponse}
 * (utilisé par la recherche filtrée) car les deux endpoints ne
 * partagent pas exactement le même contrat JSON côté API : celui-ci
 * expose {@code sellerEmail}, pas {@code sellerFirstName}, et ne
 * contient aucun champ {@code city} séparé — l'adresse complète est
 * déjà dans {@code pickupAddress}.</p>
 *
 * @param id              identifiant de l'annonce
 * @param title           titre du plat
 * @param description     description détaillée
 * @param price           prix unitaire
 * @param portions        nombre de portions disponibles
 * @param photoUrl         URL de la photo principale, {@code null} si absente
 * @param pickupAddress   adresse complète de retrait
 * @param cuisineTypeName libellé du type de cuisine
 * @param sellerEmail     email du vendeur (identifiant public v1)
 */
public record ListingSummaryResponse(
    Long id,
    String title,
    String description,
    BigDecimal price,
    Integer portions,
    String photoUrl,
    String pickupAddress,
    String cuisineTypeName,
    String sellerEmail
) {
}