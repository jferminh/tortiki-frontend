package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.user.AuthResponse;
import com.tortiki.frontend.dto.user.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour les opérations d'authentification de l'API Tortiki.
 *
 * <p>La connexion (login) elle-même est déléguée à Spring Security
 * (formulaire natif) — ce client ne gère que l'inscription, le login
 * étant intercepté par {@code SecurityConfig} avant d'atteindre ce port.</p>
 */
@FeignClient(name = "auth-api", url = "${tortiki.api.url}")
public interface AuthApiClient {

  /**
   * Crée un nouveau compte utilisateur.
   *
   * @param request données d'inscription
   * @return réponse d'authentification (id, email, rôle)
   */
  @PostMapping("/api/v1/auth/register")
  AuthResponse register(@RequestBody RegisterRequest request);
}