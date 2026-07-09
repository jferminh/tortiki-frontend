package com.tortiki.frontend.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tortiki.frontend.client.AuthApiClient;
import com.tortiki.frontend.dto.user.LoginRequest;
import com.tortiki.frontend.dto.user.UserResponse;
import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Tests unitaires du fournisseur {@link ApiDelegatingAuthenticationProvider}.
 *
 * <p>Vérifie la délégation de l'authentification à {@code tortiki-api}, la
 * gestion des erreurs Feign (401, erreurs serveur), la construction des
 * autorités Spring Security, et surtout l'extraction stricte du cookie
 * {@code JSESSIONID} depuis le {@code Set-Cookie} de la réponse
 * (correctif Issue 58 — les attributs {@code Path}/{@code HttpOnly} ne
 * doivent jamais être propagés dans l'en-tête {@code Cookie} sortant).</p>
 */
@ExtendWith(MockitoExtension.class)
@Epic("Authentification")
@Feature("Délégation vers tortiki-api")
@DisplayName("ApiDelegatingAuthenticationProvider")
class ApiDelegatingAuthenticationProviderTest {

  private static final String EMAIL = "sofia@tortiki.fr";
  private static final String PASSWORD = "S3cret!Password";

  @Mock
  private AuthApiClient authApiClient;

  private ApiDelegatingAuthenticationProvider provider;
  private MockHttpServletRequest mockRequest;

  @BeforeEach
  void setUp() {
    provider = new ApiDelegatingAuthenticationProvider(authApiClient);
    mockRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  private Authentication loginToken() {
    return new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD);
  }

  private UserResponse userResponseWithRoles(final Set<String> roles) {
    return new UserResponse(1L, EMAIL, "Sofia", "Kovalenko", roles);
  }

  private FeignException.Unauthorized unauthorizedException() {
    Request request = Request.create(HttpMethod.POST, "/api/v1/auth/login",
        Map.of(), null, new RequestTemplate());
    return new FeignException.Unauthorized("401 Unauthorized", request, null, null);
  }

  private FeignException.InternalServerError serverErrorException() {
    Request request = Request.create(HttpMethod.POST, "/api/v1/auth/login",
        Map.of(), null, new RequestTemplate());
    return new FeignException.InternalServerError("500 Internal Server Error",
        request, null, null);
  }

  @Test
  @Story("Connexion réussie")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Authentifie l'utilisateur, stocke le JSESSIONID en session "
      + "et construit les autorités Spring Security à partir des rôles API.")
  @DisplayName("authenticate stocke le cookie et construit les autorités")
  void authenticate_shouldStoreSessionCookieAndBuildAuthorities() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE,
        "JSESSIONID=A1B2C3D4E5; Path=/; HttpOnly; SameSite=Lax");
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("SELLER")), headers, HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    Authentication result = provider.authenticate(loginToken());

    assertThat(result.getName()).isEqualTo(EMAIL);
    assertThat(result.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_SELLER");
    assertThat(mockRequest.getSession(false)).isNotNull();
    assertThat(mockRequest.getSession(false)
        .getAttribute(ApiDelegatingAuthenticationProvider.SESSION_ATTR_API_COOKIE))
        .isEqualTo("JSESSIONID=A1B2C3D4E5");
  }

  @Test
  @Story("Extraction stricte du cookie")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Le cookie stocké en session ne doit contenir que la paire "
      + "JSESSIONID=valeur, jamais les attributs Path, HttpOnly ou SameSite "
      + "qui sont invalides dans un en-tête Cookie de requête sortante.")
  @DisplayName("authenticate retire les attributs Path/HttpOnly/SameSite du cookie")
  void authenticate_shouldStripCookieAttributes() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE,
        "JSESSIONID=XYZ987; Path=/; HttpOnly; Secure; SameSite=Strict");
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("BUYER")), headers, HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    provider.authenticate(loginToken());

    String storedCookie = (String) mockRequest.getSession(false)
        .getAttribute(ApiDelegatingAuthenticationProvider.SESSION_ATTR_API_COOKIE);
    assertThat(storedCookie).isEqualTo("JSESSIONID=XYZ987");
    assertThat(storedCookie).doesNotContain("Path", "HttpOnly", "SameSite", "Secure");
  }

  @Test
  @Story("Extraction stricte du cookie")
  @Severity(SeverityLevel.NORMAL)
  @Description("Si l'API renvoie plusieurs Set-Cookie, seul celui préfixé "
      + "JSESSIONID= doit être retenu — les autres cookies éventuels sont ignorés.")
  @DisplayName("authenticate sélectionne le JSESSIONID parmi plusieurs Set-Cookie")
  void authenticate_shouldPickJsessionIdAmongMultipleCookies() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, "TRACKING=abc123; Path=/");
    headers.add(HttpHeaders.SET_COOKIE, "JSESSIONID=RIGHTONE; Path=/; HttpOnly");
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("BUYER")), headers, HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    provider.authenticate(loginToken());

    String storedCookie = (String) mockRequest.getSession(false)
        .getAttribute(ApiDelegatingAuthenticationProvider.SESSION_ATTR_API_COOKIE);
    assertThat(storedCookie).isEqualTo("JSESSIONID=RIGHTONE");
  }

  @Test
  @Story("Résilience — cookie absent")
  @Severity(SeverityLevel.NORMAL)
  @Description("Si l'API ne renvoie aucun Set-Cookie, l'authentification "
      + "réussit malgré tout — un avertissement est loggé, aucune exception levée.")
  @DisplayName("authenticate tolère l'absence de Set-Cookie")
  void authenticate_shouldToleratesMissingSetCookieHeader() {
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("BUYER")), new HttpHeaders(), HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    Authentication result = provider.authenticate(loginToken());

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(mockRequest.getSession(false)).isNull();
  }

  @Test
  @Story("Résilience — cookie absent")
  @Severity(SeverityLevel.MINOR)
  @Description("Si l'API renvoie un Set-Cookie sans JSESSIONID, aucune "
      + "session n'est créée mais l'authentification n'échoue pas pour autant.")
  @DisplayName("authenticate tolère un Set-Cookie sans JSESSIONID")
  void authenticate_shouldToleratesSetCookieWithoutJsessionId() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, "TRACKING=abc123; Path=/");
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("BUYER")), headers, HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    Authentication result = provider.authenticate(loginToken());

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(mockRequest.getSession(false)).isNull();
  }

  @Test
  @Story("Échec d'authentification")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Un 401 de l'API se traduit par BadCredentialsException, "
      + "sans révéler si c'est l'email ou le mot de passe qui est incorrect.")
  @DisplayName("authenticate lève BadCredentialsException sur 401")
  void authenticate_shouldThrowBadCredentialsOn401() {
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD)))
        .thenThrow(unauthorizedException());

    assertThatThrownBy(() -> provider.authenticate(loginToken()))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Email ou mot de passe incorrect.");
  }

  @Test
  @Story("Échec d'authentification")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Toute autre erreur Feign (ex. 500) se traduit par "
      + "AuthenticationServiceException, message générique sans détail technique exposé.")
  @DisplayName("authenticate lève AuthenticationServiceException sur erreur serveur")
  void authenticate_shouldThrowServiceExceptionOnServerError() {
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD)))
        .thenThrow(serverErrorException());

    assertThatThrownBy(() -> provider.authenticate(loginToken()))
        .isInstanceOf(org.springframework.security.authentication.AuthenticationServiceException.class)
        .hasMessage("Le service d'authentification est momentanément indisponible.");
  }

  @Test
  @Story("Échec d'authentification")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un corps de réponse vide (body null) est un cas anormal — "
      + "l'authentification échoue explicitement plutôt que de créer un utilisateur incomplet.")
  @DisplayName("authenticate lève une exception si le corps de réponse est vide")
  void authenticate_shouldThrowWhenBodyIsNull() {
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        (UserResponse) null, new HttpHeaders(), HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    assertThatThrownBy(() -> provider.authenticate(loginToken()))
        .isInstanceOf(AuthenticationServiceException.class)
        .hasMessage("Réponse d'authentification vide.");
  }

  @Test
  @Story("Échec d'authentification")
  @Severity(SeverityLevel.NORMAL)
  @Description("Une réponse sans rôle exploitable (liste vide ou nulle) "
      + "doit échouer plutôt que de créer une Authentication sans autorité, "
      + "ce qui bloquerait silencieusement tout contrôle d'accès en aval.")
  @DisplayName("authenticate lève une exception si aucun rôle exploitable")
  void authenticate_shouldThrowWhenNoExploitableRole() {
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of()), new HttpHeaders(), HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    assertThatThrownBy(() -> provider.authenticate(loginToken()))
        .isInstanceOf(AuthenticationServiceException.class)
        .hasMessage("Réponse d'authentification sans rôle exploitable.");
  }

  @Test
  @Story("Normalisation des rôles")
  @Severity(SeverityLevel.NORMAL)
  @Description("Les rôles bruts de l'API sont triés, dédupliqués et "
      + "préfixés ROLE_ uniquement s'ils ne le sont pas déjà, conformément "
      + "au format attendu par SimpleGrantedAuthority.")
  @DisplayName("authenticate normalise, filtre et déduplique les rôles bruts")
  void authenticate_shouldNormalizeFilterAndDeduplicateRoles() {
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("SELLER", "ROLE_SELLER", " ", "  BUYER  ")),
        new HttpHeaders(), HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    Authentication result = provider.authenticate(loginToken());

    assertThat(result.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_SELLER", "ROLE_BUYER");
  }

  @Test
  @Story("Résilience — contexte HTTP absent")
  @Severity(SeverityLevel.MINOR)
  @Description("Si aucun contexte de requête HTTP n'est actif (ex. appel "
      + "hors thread web), le stockage du cookie est ignoré sans faire "
      + "échouer l'authentification — un cas défensif improbable en production.")
  @DisplayName("authenticate tolère l'absence de contexte de requête HTTP")
  void authenticate_shouldToleratesMissingRequestContext() {
    RequestContextHolder.resetRequestAttributes();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, "JSESSIONID=ABC123; Path=/");
    ResponseEntity<UserResponse> response = new ResponseEntity<>(
        userResponseWithRoles(Set.of("BUYER")), headers, HttpStatus.OK);
    when(authApiClient.login(new LoginRequest(EMAIL, PASSWORD))).thenReturn(response);

    Authentication result = provider.authenticate(loginToken());

    assertThat(result.isAuthenticated()).isTrue();
  }

  @Test
  @Story("Contrat AuthenticationProvider")
  @Severity(SeverityLevel.MINOR)
  @Description("Le provider ne doit supporter que "
      + "UsernamePasswordAuthenticationToken, seul type produit par formLogin.")
  @DisplayName("supports accepte uniquement UsernamePasswordAuthenticationToken")
  void supports_shouldAcceptOnlyUsernamePasswordAuthenticationToken() {
    assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    assertThat(provider.supports(mock(Authentication.class).getClass())).isFalse();
  }

  @Test
  @Story("Contrat AuthenticationProvider")
  @Severity(SeverityLevel.MINOR)
  @Description("Aucun appel à AuthApiClient ne doit être déclenché par "
      + "un simple test de compatibilité de type (supports).")
  @DisplayName("supports n'interagit jamais avec AuthApiClient")
  void supports_shouldNeverInteractWithAuthApiClient() {
    provider.supports(UsernamePasswordAuthenticationToken.class);

    verifyNoInteractions(authApiClient);
  }
}