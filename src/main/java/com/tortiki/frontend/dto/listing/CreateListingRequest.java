package com.tortiki.frontend.dto.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Données de création d'une annonce, saisies par le vendeur.
 *
 * @param title titre du plat
 * @param description description détaillée
 * @param price prix unitaire en euros
 * @param portions nombre de portions disponibles
 * @param pickupAddress adresse complète de retrait
 * @param pickupDatetime créneau de retrait
 * @param cuisineTypeId identifiant du type de cuisine
 * @param allergenIds identifiants des allergènes présents
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
    Integer portions,

    @NotBlank(message = "L'adresse de retrait est obligatoire")
    String pickupAddress,

    @NotNull(message = "Le créneau de retrait est obligatoire")
    @Future(message = "Le créneau de retrait doit être dans le futur")
    LocalDateTime pickupDatetime,

    @NotNull(message = "Le type de cuisine est obligatoire")
    Long cuisineTypeId,

    List<Long> allergenIds
) {}