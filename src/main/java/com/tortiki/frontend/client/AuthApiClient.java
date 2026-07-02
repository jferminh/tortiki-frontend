package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.user.AuthResponse;
import com.tortiki.frontend.dto.user.LoginRequest;
import com.tortiki.frontend.dto.user.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour les opérations d'authentification de l'API Tortiki.
 *
 * <p>Le login est désormais délégué explicitement à l'API via
 * {@link #login(LoginRequest)} — {@code ApiDelegatingAuthenticationProvider}
 * consomme cette méthode pour vérifier les identifiants et récupérer
 * le cookie de session {@code JSESSIONID} de l'API.</p>
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

  /**
   * Authentifie un utilisateur auprès de l'API et retourne
   * la réponse complète (headers inclus) pour récupérer le cookie
   * {@code Set-Cookie} contenant le JSESSIONID de l'API.
   *
   * @param request identifiants de connexion
   * @return réponse HTTP complète avec headers et corps {@code AuthResponse}
   */
  @PostMapping("/api/v1/auth/login")
  ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request);
}