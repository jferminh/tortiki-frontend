package com.tortiki.frontend.dto.search;

/**
 * Critères de recherche d'annonces saisis par l'utilisateur.
 *
 * <p>Record immuable Java 21 — reçu depuis les paramètres de requête
 * de {@code GET /search} et transmis à {@code SearchApiClient}.</p>
 *
 * @param query mot-clé libre (titre, description)
 * @param city ville de recherche
 * @param postalCode code postal
 * @param cuisineTypeId filtre par type de cuisine
 * @param page numéro de page (0-based)
 * @param size nombre de résultats par page
 */
public record SearchCriteria(
    String query,
    String city,
    String postalCode,
    Long cuisineTypeId,
    int page,
    int size
) {}