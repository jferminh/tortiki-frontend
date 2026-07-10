package com.tortiki.frontend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * Redirige l'utilisateur après connexion selon son rôle principal.
 *
 * <p>Sofia (SELLER) est redirigée vers son tableau de bord, Théo (BUYER)
 * et les autres profils vers la page d'accueil. Évite de forcer tous
 * les rôles vers une même route générique après authentification.</p>
 */
@Slf4j
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final String ROUTE_DASHBOARD = "/dashboard";
  private static final String ROUTE_HOME = "/";
  private static final String ROLE_SELLER = "ROLE_SELLER";

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final Authentication authentication)
      throws IOException, ServletException {
    final boolean isSeller = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(ROLE_SELLER::equals);
    final String targetUrl = isSeller ? ROUTE_DASHBOARD : ROUTE_HOME;
    log.info("Connexion réussie pour {}, redirection vers {}",
        authentication.getName(), targetUrl);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}