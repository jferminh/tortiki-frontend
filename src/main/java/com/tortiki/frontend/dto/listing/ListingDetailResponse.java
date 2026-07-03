package com.tortiki.frontend.dto.listing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail complet d'une annonce, utilisé pour préremplir
 * le formulaire d'édition.
 *
 * @param id identifiant de l'annonce
 * @param title titre du plat
 * @param description description détaillée
 * @param price prix unitaire
 * @param portions portions disponibles
 * @param cuisineTypeId type de cuisine
 * @param allergenIds allergènes présents
 * @param pickupAddress ville de retrait
 * @param photoUrl URL de la photo actuelle
 */
public record ListingDetailResponse(
    Long id,
    String title,
    String description,
    BigDecimal price,
    Integer portions,
    Long cuisineTypeId,
    List<Long> allergenIds,
    String pickupAddress,
    LocalDateTime pickupDatetime,
    String photoUrl
) {}