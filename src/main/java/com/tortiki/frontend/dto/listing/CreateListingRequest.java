package com.tortiki.frontend.dto.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Données de création d'une annonce, saisies par le vendeur.
 *
 * @param title titre du plat
 * @param description description détaillée
 * @param price prix unitaire en euros
 * @param portionsAvailable nombre de portions disponibles
 * @param cuisineTypeId identifiant du type de cuisine
 * @param allergenIds identifiants des allergènes présents
 * @param city ville de retrait
 */
public record CreateListingRequest(
    @NotBlank(message = "Le titre est obligatoire")
    String title,

    @NotBlank(message = "La description est obligatoire")
    String description,

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.50", message = "Le prix minimum est de 0,50 €")
    BigDecimal price,

    @NotNull(message = "Le nombre de portions est obligatoire")
    @Positive(message = "Le nombre de portions doit être positif")
    Integer portionsAvailable,

    @NotNull(message = "Le type de cuisine est obligatoire")
    Long cuisineTypeId,

    List<Long> allergenIds,

    @NotBlank(message = "La ville est obligatoire")
    String city
) {}