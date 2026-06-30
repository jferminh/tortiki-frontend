package com.tortiki.frontend.dto.listing;

import java.math.BigDecimal;

/**
 * DTO de réponse représentant une annonce en format carte.
 *
 * <p>Record immuable Java 21 — désérialisé depuis la réponse JSON
 * de {@code GET /api/v1/listings/search}.</p>
 *
 * <p>Ce DTO est volontairement allégé : il ne contient que les données
 * nécessaires à l'affichage d'une carte dans les résultats de recherche.
 * La fiche complète est chargée séparément via {@code ListingApiClient}.</p>
 *
 * @param id              identifiant de l'annonce
 * @param title           titre du plat
 * @param description     description courte
 * @param price           prix unitaire
 * @param portions        nombre de portions disponibles
 * @param photoUrl        URL de la photo principale (MinIO)
 * @param pickupAddress   adresse de retrait
 * @param cuisineTypeName libellé du type de cuisine
 * @param sellerFirstName prénom du vendeur
 * @param city            ville du vendeur
 */
public record ListingCardResponse(
    Long id,
    String title,
    String description,
    BigDecimal price,
    Integer portions,
    String photoUrl,
    String pickupAddress,
    String cuisineTypeName,
    String sellerFirstName,
    String city
) {
}
