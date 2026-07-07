package com.tortiki.frontend.dto.listing;

import jakarta.validation.constraints.NotBlank;

/**
 * Données de création d'un type de cuisine, saisies par l'administrateur.
 *
 * <p>Ce record appartient à {@code dto/listing} et non à {@code dto/admin},
 * car il représente un contrat métier partagé (le référentiel des types de
 * cuisine), au même titre que {@link CuisineTypeResponse}. Seule l'opération
 * de création est réservée à l'administrateur ; le type lui-même n'est pas
 * une notion "admin".</p>
 *
 * @param name        libellé du type de cuisine (ex : "Ukrainienne", "Maghrébine")
 * @param description description courte de l'origine culinaire
 */
public record CreateCuisineTypeRequest(
    @NotBlank(message = "Le nom est obligatoire")
    String name,

    @NotBlank(message = "La description est obligatoire")
    String description) {
}