package com.tortiki.frontend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Données saisies lors de la création d'un compte utilisateur.
 *
 * <p>Record immuable Java 21 — validé via Bean Validation avant
 * transmission à {@code AuthApiClient.register}.</p>
 *
 * @param firstName prénom de l'utilisateur
 * @param lastName nom de l'utilisateur
 * @param email adresse e-mail (identifiant de connexion)
 * @param password mot de passe en clair (haché côté API par BCrypt)
 * @param role rôle souhaité : SELLER ou BUYER
 */
public record RegisterRequest(
    @NotBlank(message = "Le prénom est obligatoire")
    String firstName,

    @NotBlank(message = "Le nom est obligatoire")
    String lastName,

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String password,

    @NotBlank(message = "Le rôle est obligatoire")
    String role
) {}