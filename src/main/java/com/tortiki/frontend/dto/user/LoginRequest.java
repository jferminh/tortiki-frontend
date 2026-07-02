package com.tortiki.frontend.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Données de connexion transmises à {@code tortiki-api}.
 *
 * @param email adresse e-mail de connexion
 * @param password mot de passe en clair (transmis en HTTPS uniquement)
 */
public record LoginRequest(
    @NotBlank(message = "L'email est obligatoire")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    String password
) {}