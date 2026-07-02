package com.tortiki.frontend.dto.user;

/**
 * Réponse de l'API après inscription ou connexion réussie.
 *
 * @param id identifiant utilisateur
 * @param email email de l'utilisateur
 * @param role rôle attribué
 */
public record AuthResponse(
    Long id,
    String email,
    String role
) {}