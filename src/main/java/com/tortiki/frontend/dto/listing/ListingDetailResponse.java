package com.tortiki.frontend.dto.listing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail complet d'une annonce.
 *
 * <p>Utilisé pour deux usages distincts : préremplir le formulaire
 * d'édition ({@code SellerListingController#editListingForm}) et
 * afficher le dashboard vendeur avec badge de statut
 * ({@code SellerListingController#myListings}).</p>
 *
 * @param id identifiant de l'annonce
 * @param title titre du plat
 * @param description description détaillée
 * @param price prix unitaire
 * @param portions portions disponibles
 * @param cuisineTypeId type de cuisine
 * @param allergenIds allergènes présents
 * @param pickupAddress adresse de retrait
 * @param pickupDatetime créneau de retrait
 * @param photoUrl URL de la photo actuelle
 * @param status statut de l'annonce ({@code ACTIVE}, {@code INACTIVE}, {@code MODERATED})
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
    String photoUrl,
    String status
) {}