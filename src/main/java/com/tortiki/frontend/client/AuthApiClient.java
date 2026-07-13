package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.user.LoginRequest;
import com.tortiki.frontend.dto.user.RegisterRequest;
import com.tortiki.frontend.dto.user.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour les opérations d'authentification auprès de {@code tortiki-api}.
 *
 * <p>{@code UserResponse} sert de contrat unique pour {@code login} et {@code register} :
 * les deux endpoints renvoient désormais la même représentation de l'utilisateur
 * (identité + rôles), ce qui élimine la duplication qu'aurait entraînée un
 * {@code AuthResponse} distinct — principe DRY.</p>
 *
 * <p>{@code ResponseEntity} est utilisé plutôt qu'un type de retour brut pour
 * {@code login}, car {@code ApiDelegatingAuthenticationProvider} a besoin d'accéder
 * à l'en-tête {@code Set-Cookie} de la réponse afin de relier la session frontend
 * à la session API (voir Issue 58).</p>
 */
@FeignClient(name = "auth-api", url = "${tortiki.api.url}")
public interface AuthApiClient {

  /**
   * Authentifie un utilisateur auprès de l'API.
   *
   * @param request identifiants de connexion (email, mot de passe)
   * @return réponse HTTP complète, incluant le cookie de session et le profil utilisateur
   */
  @PostMapping("/api/v1/auth/login")
  ResponseEntity<UserResponse> login(@RequestBody LoginRequest request);

  /**
   * Inscrit un nouvel utilisateur auprès de l'API.
   *
   * @param request données d'inscription (email, mot de passe, nom, rôle)
   * @return profil de l'utilisateur nouvellement créé
   */
  @PostMapping("/api/v1/auth/register")
  UserResponse register(@RequestBody RegisterRequest request);

  /**
   * Déconnecte l'utilisateur en invalidant sa session côté API.
   *
   * <p>Le cookie de session API est relayé automatiquement par
   * {@code FeignConfig.sessionCookieInterceptor}, qui lit l'attribut stocké en
   * session frontend — aucun paramètre explicite n'est donc nécessaire ici.</p>
   */
  @PostMapping("/api/v1/auth/logout")
  void logout();
}