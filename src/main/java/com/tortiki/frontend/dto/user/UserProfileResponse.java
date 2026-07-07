package com.tortiki.frontend.dto.user;

import java.util.Set;

/**
 * Détail du profil de l'utilisateur connecté.
 *
 * <p>Reflète {@code UserResponse} côté {@code tortiki-api} : aucune donnée
 * sensible (hash de mot de passe) n'est jamais exposée au frontend.</p>
 *
 * @param id        identifiant technique
 * @param email     adresse email, identifiant de connexion
 * @param firstName prénom
 * @param lastName  nom de famille
 * @param roles     rôles attribués (ex : SELLER, BUYER)
 */
public record UserProfileResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {
}