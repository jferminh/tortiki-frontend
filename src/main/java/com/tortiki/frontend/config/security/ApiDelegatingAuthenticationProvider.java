package com.tortiki.frontend.config.security;

import com.tortiki.frontend.client.AuthApiClient;
import com.tortiki.frontend.dto.user.AuthResponse;
import com.tortiki.frontend.dto.user.LoginRequest;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
 *   <li>Récupère le cookie {@code Set-Cookie} (JSESSIONID de l'API) dans
 *       la réponse HTTP.</li>
 *   <li>Stocke ce cookie dans la session HTTP du frontend, pour que
 *       {@code FeignConfig} le réinjecte lors des appels Feign suivants.</li>
 *   <li>Construit une {@code Authentication} Spring Security avec le rôle
 *       reçu depuis l'API.</li>
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

  /** Nom de l'attribut de session stockant le cookie de session API. */
  public static final String SESSION_ATTR_API_COOKIE = "API_SESSION_COOKIE";

  /** Client Feign vers les endpoints d'authentification de l'API. */
  private final AuthApiClient authApiClient;

  /**
   * Authentifie l'utilisateur en déléguant la vérification à l'API,
   * puis relie la session frontend à la session API via le cookie reçu.
   *
   * @param authentication jeton contenant email (principal) et mot de passe (credentials)
   * @return authentification complète avec rôle attribué par l'API
   * @throws AuthenticationException si les identifiants sont invalides
   *     ou si l'API est injoignable
   */
  @Override
  public Authentication authenticate(final Authentication authentication)
      throws AuthenticationException {
    String email = authentication.getName();
    String password = authentication.getCredentials().toString();

    ResponseEntity<AuthResponse> response;
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

    AuthResponse body = response.getBody();
    if (body == null) {
      throw new AuthenticationServiceException("Réponse d'authentification vide.");
    }

    return new UsernamePasswordAuthenticationToken(
        email,
        password,
        List.of(new SimpleGrantedAuthority("ROLE_" + body.role()))
    );
  }

  /**
   * Extrait le cookie {@code Set-Cookie} de la réponse API et le stocke
   * dans la session HTTP courante du frontend.
   *
   * @param headers en-têtes de la réponse HTTP de l'API
   */
  private void storeApiSessionCookie(final HttpHeaders headers) {
    String setCookie = headers.getFirst(HttpHeaders.SET_COOKIE);
    if (setCookie == null) {
      log.warn("Aucun cookie Set-Cookie reçu de l'API après connexion réussie.");
      return;
    }
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      log.error("Impossible de stocker le cookie API : aucun contexte de requête HTTP actif.");
      return;
    }
    HttpServletRequest request = attributes.getRequest();
    request.getSession(true).setAttribute(SESSION_ATTR_API_COOKIE, setCookie);
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