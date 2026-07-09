package com.tortiki.frontend.config.security;

import com.tortiki.frontend.client.AuthApiClient;
import com.tortiki.frontend.dto.user.LoginRequest;
import com.tortiki.frontend.dto.user.UserResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Fournisseur d'authentification qui délègue la vérification des identifiants
 * à {@code tortiki-api} plutôt qu'à un {@code UserDetailsService} local.
 *
 * <p>Le frontend ne possède aucune base d'utilisateurs propre — toute vérité
 * sur les comptes réside dans {@code tortiki-api}. Ce provider :</p>
 * <ol>
 *   <li>Transmet email/mot de passe à {@code AuthApiClient.login}.</li>
 *   <li>Extrait strictement la paire {@code JSESSIONID=valeur} du
 *       {@code Set-Cookie} de la réponse — sans les attributs
 *       {@code Path}, {@code HttpOnly} ou {@code SameSite}.</li>
 *   <li>Stocke ce cookie dans la session HTTP du frontend, pour que
 *       {@code FeignConfig} le réinjecte lors des appels Feign suivants.</li>
 *   <li>Construit une {@code Authentication} Spring Security avec les rôles
 *       reçus depuis l'API.</li>
 * </ol>
 *
 * <p>Sécurité : le cookie API n'est jamais transmis au navigateur — il reste
 * exclusivement côté serveur, dans l'attribut de session HTTP du frontend.
 * Conforme OWASP A07 (Identification and Authentication Failures).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiDelegatingAuthenticationProvider implements AuthenticationProvider {

  /**
   * Nom de l'attribut de session stockant le cookie de session API.
   */
  public static final String SESSION_ATTR_API_COOKIE = "API_SESSION_COOKIE";

  /**
   * Préfixe standard Spring Security pour les autorités de rôle.
   */
  private static final String ROLE_PREFIX = "ROLE_";

  /**
   * Nom du cookie de session porté par {@code tortiki-api}.
   */
  private static final String JSESSIONID_PREFIX = "JSESSIONID=";

  /**
   * Client Feign vers les endpoints d'authentification de l'API.
   */
  private final AuthApiClient authApiClient;

  /**
   * Authentifie l'utilisateur en déléguant la vérification à l'API,
   * puis relie la session frontend à la session API via le cookie reçu.
   *
   * @param authentication jeton contenant email (principal) et mot de passe (credentials)
   * @return authentification complète avec rôles attribués par l'API
   * @throws AuthenticationException si les identifiants sont invalides
   *                                 ou si l'API est injoignable
   */
  @Override
  public Authentication authenticate(final Authentication authentication)
      throws AuthenticationException {
    String email = authentication.getName();
    String password = authentication.getCredentials().toString();

    ResponseEntity<UserResponse> response;
    try {
      response = authApiClient.login(new LoginRequest(email, password));
    } catch (FeignException.Unauthorized unauthorizedException) {
      log.warn("Échec de connexion pour l'email : {}", email);
      throw new BadCredentialsException("Email ou mot de passe incorrect.");
    } catch (FeignException feignException) {
      log.error("Erreur API lors de la tentative de connexion : {}",
          feignException.getMessage());
      throw new AuthenticationServiceException(
          "Le service d'authentification est momentanément indisponible.");
    }

    storeApiSessionCookie(response.getHeaders());

    UserResponse body = response.getBody();
    if (body == null) {
      throw new AuthenticationServiceException("Réponse d'authentification vide.");
    }

    List<SimpleGrantedAuthority> authorities = buildAuthorities(body.roles());

    return new UsernamePasswordAuthenticationToken(
        email,
        password,
        authorities
    );
  }

  /**
   * Convertit la collection de rôles renvoyée par l'API
   * en autorités Spring Security.
   *
   * @param roles rôles fonctionnels renvoyés par l'API
   * @return autorités Spring Security normalisées
   */
  private List<SimpleGrantedAuthority> buildAuthorities(final Collection<String> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new AuthenticationServiceException(
          "Réponse d'authentification sans rôle exploitable.");
    }

    List<SimpleGrantedAuthority> authorities = roles.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(role -> !role.isBlank())
        .map(this::normalizeRole)
        .distinct()
        .map(SimpleGrantedAuthority::new)
        .toList();

    if (authorities.isEmpty()) {
      throw new AuthenticationServiceException(
          "Réponse d'authentification sans rôle exploitable.");
    }

    return authorities;
  }

  /**
   * Normalise un rôle pour respecter le format attendu par Spring Security.
   *
   * @param role rôle brut retourné par l'API
   * @return rôle préfixé par {@code ROLE_} si nécessaire
   */
  private String normalizeRole(final String role) {
    if (role.startsWith(ROLE_PREFIX)) {
      return role;
    }
    return ROLE_PREFIX + role;
  }

  /**
   * Extrait le cookie {@code JSESSIONID} de la réponse API et le stocke
   * dans la session HTTP courante du frontend.
   *
   * <p>Seule la paire {@code nom=valeur} est conservée : les attributs
   * {@code Path}, {@code HttpOnly} et {@code SameSite} du {@code Set-Cookie}
   * ne sont pas valides dans un en-tête {@code Cookie} de requête sortante,
   * et un serveur conforme RFC 6265 ne renvoie qu'un seul {@code Set-Cookie}
   * par nom de cookie.</p>
   *
   * @param headers en-têtes de la réponse HTTP de l'API
   */
  private void storeApiSessionCookie(final HttpHeaders headers) {
    List<String> setCookieHeaders = headers.get(HttpHeaders.SET_COOKIE);
    if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
      log.warn("Aucun cookie Set-Cookie reçu de l'API après connexion réussie.");
      return;
    }

    String sessionCookie = setCookieHeaders.stream()
        .filter(header -> header.startsWith(JSESSIONID_PREFIX))
        .findFirst()
        .map(header -> header.split(";", 2)[0])
        .orElse(null);

    if (sessionCookie == null) {
      log.warn("Aucun JSESSIONID trouvé parmi les cookies renvoyés par l'API.");
      return;
    }

    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      log.error("Impossible de stocker le cookie API : aucun contexte de requête HTTP actif.");
      return;
    }

    HttpServletRequest request = attributes.getRequest();
    request.getSession(true).setAttribute(SESSION_ATTR_API_COOKIE, sessionCookie);
    log.debug("Cookie de session API stocké dans la session frontend.");
  }

  /**
   * Indique que ce provider gère les authentifications par identifiant/mot de passe.
   *
   * @param authentication classe du jeton d'authentification
   * @return {@code true} si le type est supporté
   */
  @Override
  public boolean supports(final Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}