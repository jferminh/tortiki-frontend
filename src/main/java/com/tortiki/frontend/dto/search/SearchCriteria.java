package com.tortiki.frontend.dto.search;

/**
 * Critères de recherche d'annonces saisis par l'utilisateur.
 *
 * <p>Record immuable Java 21 — reçu depuis les paramètres de requête
 * de {@code GET /search} et transmis à {@code SearchApiClient}.</p>
 *
 * <p>{@code page} et {@code size} sont typés {@link Integer} (boxed)
 * et non {@code int} primitif : lorsqu'un visiteur soumet une recherche
 * sans préciser explicitement la pagination (cas le plus fréquent),
 * Spring Data Binding transmet {@code null} au lieu d'échouer avec une
 * {@code MethodArgumentNotValidException} sur un type primitif non
 * nullable. Les valeurs par défaut sont appliquées dans le contrôleur.</p>
 *
 * @param query mot-clé libre (titre, description)
 * @param city ville de recherche
 * @param postalCode code postal
 * @param cuisineTypeId filtre par type de cuisine
 * @param page numéro de page (0-based), {@code null} si non précisé
 * @param size nombre de résultats par page, {@code null} si non précisé
 */
public record SearchCriteria(
    String query,
    String city,
    String postalCode,
    Long cuisineTypeId,
    Integer page,
    Integer size
) {}