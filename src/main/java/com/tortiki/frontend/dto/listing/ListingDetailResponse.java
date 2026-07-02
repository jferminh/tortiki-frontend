package com.tortiki.frontend.dto.listing;

/**
 * Détail complet d'une annonce, utilisé pour préremplir
 * le formulaire d'édition.
 *
 * @param id identifiant de l'annonce
 * @param title titre du plat
 * @param description description détaillée
 * @param price prix unitaire
 * @param portionsAvailable portions disponibles
 * @param cuisineTypeId type de cuisine
 * @param allergenIds allergènes présents
 * @param city ville de retrait
 * @param photoUrl URL de la photo actuelle
 */
public record ListingDetailResponse(
    Long id,
    String title,
    String description,
    java.math.BigDecimal price,
    Integer portionsAvailable,
    Long cuisineTypeId,
    java.util.List<Long> allergenIds,
    String city,
    String photoUrl
) {}