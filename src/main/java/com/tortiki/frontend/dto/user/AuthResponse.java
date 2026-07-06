package com.tortiki.frontend.dto.user;

import java.util.Set;

/**
 * Réponse de l'API après inscription ou connexion réussie.
 *
 * <p>Correspond exactement au contrat JSON de {@code UserResponse} dans tortiki-api :
 * le champ {@code roles} est un {@code Set<String>} contenant les noms des rôles
 * (ex. : {@code "SELLER"}, {@code "BUYER"}, {@code "ADMIN"}).</p>
 *
 * @param id       identifiant utilisateur
 * @param email    email de l'utilisateur
 * @param firstName prénom de l'utilisateur
 * @param lastName  nom de famille de l'utilisateur
 * @param roles    ensemble des rôles attribués
 */
public record AuthResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {}