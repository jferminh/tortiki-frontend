package com.tortiki.frontend.config.security;

import com.tortiki.frontend.client.AuthApiClient;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Gestionnaire de déconnexion qui invalide explicitement la session
 * côté tortiki-api avant que la session frontend ne soit détruite.
 *
 * <p>Sans ce handler, seule la session locale du frontend est invalidée
 * par {@code SecurityConfig.logout()} : la session ouverte côté API
 * reste active indéfiniment côté serveur, ce qui constitue une fuite
 * de session non nettoyée (violation OWASP A07).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLogoutHandler implements LogoutHandler {

  private final AuthApiClient authApiClient;

  /**
   * Relaie la déconnexion à l'API avant l'invalidation locale.
   *
   * @param request        requête HTTP courante
   * @param response       réponse HTTP courante, non utilisée ici
   * @param authentication authentification active, peut être {@code null}
   */
  @Override
  public void logout(final HttpServletRequest request,
                     final HttpServletResponse response,
                     final Authentication authentication) {
    final HttpSession session = request.getSession(false);
    if (session == null) {
      log.debug("Aucune session frontend active, aucun appel API de déconnexion nécessaire.");
      return;
    }

    final Object apiCookie = session.getAttribute(
        ApiDelegatingAuthenticationProvider.SESSION_ATTR_API_COOKIE);

    if (apiCookie == null) {
      log.debug("Aucun cookie de session API en session, aucun appel de déconnexion nécessaire.");
      return;
    }

    try {
      authApiClient.logout();
      log.info("Session API invalidée avec succès pour {}", authentication != null
          ? authentication.getName() : "utilisateur inconnu");
    } catch (final FeignException feignException) {
      log.warn("Échec de l'invalidation de la session API : {}", feignException.getMessage());
    }
  }
}