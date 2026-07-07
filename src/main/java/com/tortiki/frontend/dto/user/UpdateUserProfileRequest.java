package com.tortiki.frontend.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Commande de mise à jour du profil utilisateur.
 *
 * <p>L'email n'est volontairement pas modifiable ici : c'est l'identifiant
 * de connexion résolu par la session, le faire évoluer nécessiterait un
 * flux de vérification dédié, hors périmètre de cette issue.</p>
 *
 * @param firstName nouveau prénom
 * @param lastName  nouveau nom de famille
 */
public record UpdateUserProfileRequest(
    @NotBlank(message = "Le prénom est obligatoire") String firstName,
    @NotBlank(message = "Le nom est obligatoire") String lastName
) {
}