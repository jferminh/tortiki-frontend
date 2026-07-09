package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.user.UpdateUserProfileRequest;
import com.tortiki.frontend.dto.user.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour la consultation et la mise à jour du profil utilisateur.
 *
 * <p>Consomme {@code GET /api/v1/users/me} et {@code PUT /api/v1/users/me}
 * de {@code tortiki-api}. L'identité de l'utilisateur connecté n'est jamais
 * transmise en paramètre : elle est résolue côté API depuis le cookie de
 * session relayé par {@code FeignConfig#sessionCookieInterceptor}, comme
 * pour {@link ListingApiClient#getMyListings()}.</p>
 */
@FeignClient(name = "user-api", url = "${tortiki.api.url}")
public interface UserApiClient {

  /**
   * Récupère le profil de l'utilisateur actuellement connecté.
   *
   * @return détail du profil (identité, rôles)
   */
  @GetMapping("/api/v1/users/me")
  UserResponse getMyProfile();

  /**
   * Met à jour le profil de l'utilisateur actuellement connecté.
   *
   * @param request nouvelles informations de profil
   * @return profil mis à jour
   */
  @PutMapping("/api/v1/users/me")
  UserResponse updateMyProfile(@RequestBody UpdateUserProfileRequest request);
}