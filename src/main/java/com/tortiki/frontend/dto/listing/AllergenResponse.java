package com.tortiki.frontend.dto.listing;

/**
 * Représentation d'un allergène pour affichage dans le formulaire vendeur.
 *
 * @param id identifiant de l'allergène
 * @param name nom de l'allergène
 */
public record AllergenResponse(Long id, String name) {}