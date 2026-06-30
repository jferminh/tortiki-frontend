package com.tortiki.frontend.dto.listing;

/**
 * DTO de réponse représentant un type de cuisine.
 *
 * <p>Record immuable Java 21 — désérialisé depuis la réponse JSON
 * de {@code GET /api/v1/cuisine-types}.</p>
 *
 * @param id identifiant technique
 * @param name libellé du type de cuisine (ex : Ukrainien, Maghrébin)
 * @param enabled indique si le type est actif sur la plateforme
 */
public record CuisineTypeResponse(
  Long id,
  String name,
  boolean enabled
) {}
