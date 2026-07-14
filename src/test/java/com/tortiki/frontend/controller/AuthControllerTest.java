package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.AuthApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.config.security.ApiLogoutHandler;
import com.tortiki.frontend.dto.user.RegisterRequest;
import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests unitaires de la couche web {@code AuthController}.
 *
 * <p>{@code AuthApiClient} est simulé via {@code @MockitoBean}. {@code
 * SecurityConfig} est importé explicitement (comme pour {@code
 * HomeControllerTest}) afin de vérifier les vraies règles {@code
 * permitAll()} de {@code /login} et {@code /register}, ainsi que la
 * protection CSRF réelle sur le formulaire d'inscription.</p>
 *
 * <p>{@code ApiLogoutHandler} est également simulé : {@code @WebMvcTest}
 * ne scanne pas les {@code @Component} hors couche web, alors que {@code
 * SecurityConfig#securityFilterChain} en dépend désormais pour le logout
 * délégué à l'API. Sans ce mock, le contexte Spring ne démarre pas.</p>
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Authentification")
class AuthControllerTest {

  private static final String LOGIN_URL = "/login";
  private static final String REGISTER_URL = "/register";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthApiClient authApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @MockitoBean
  private ApiLogoutHandler apiLogoutHandler;

  @Test
  @DisplayName("GET /login retourne 200 et affiche le formulaire de connexion")
  @Story("Accès aux formulaires publics")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie que /login est accessible sans authentification "
      + "(règle permitAll de SecurityConfig) et affiche la vue login.")
  void shouldReturnLoginFormForAnonymousUser() throws Exception {
    mockMvc.perform(get(LOGIN_URL))
        .andExpect(status().isOk())
        .andExpect(view().name("login"));
  }

  @Test
  @DisplayName("GET /register retourne 200 avec un formulaire pré-rempli en BUYER")
  @Story("Inscription")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo (visiteur) affiche le formulaire d'inscription vide, "
      + "pré-rempli avec le rôle par défaut BUYER conformément au contrôleur.")
  void shouldReturnEmptyRegisterForm() throws Exception {
    mockMvc.perform(get(REGISTER_URL))
        .andExpect(status().isOk())
        .andExpect(view().name("register"))
        .andExpect(model().attribute("registerRequest",
            new RegisterRequest("", "", "", "", "BUYER")));
  }

  @Test
  @DisplayName("POST /register avec des données valides redirige vers /login")
  @Story("Inscription")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia s'inscrit avec des données valides : le contrôleur délègue "
      + "à AuthApiClient.register puis redirige vers /login avec un message flash.")
  void shouldRedirectToLoginAfterSuccessfulRegistration() throws Exception {
    final ResultActions result = whenSubmitRegisterForm(validRegisterFormParams());

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LOGIN_URL))
        .andExpect(flash().attributeExists("success"));
    verify(authApiClient).register(any(RegisterRequest.class));
  }

  @Test
  @DisplayName("POST /register avec un email invalide réaffiche le formulaire")
  @Story("Inscription — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un email au format invalide doit être rejeté par Bean Validation "
      + "avant tout appel à AuthApiClient, avec réaffichage du formulaire (200).")
  void shouldRedisplayFormWhenEmailIsInvalid() throws Exception {
    final ResultActions result = whenSubmitRegisterForm(
        validRegisterFormParams().replace("email=sofia%40tortiki.fr", "email=invalide"));

    result.andExpect(status().isOk())
        .andExpect(view().name("register"));
  }

  @Test
  @DisplayName("POST /register avec un email déjà utilisé réaffiche le formulaire")
  @Story("Inscription — conflit")
  @Severity(SeverityLevel.NORMAL)
  @Description("Si tortiki-api répond 409 (email déjà utilisé), le contrôleur "
      + "rejette le champ email et réaffiche le formulaire sans redirection.")
  void shouldRedisplayFormWhenEmailAlreadyExists() throws Exception {
    when(authApiClient.register(any(RegisterRequest.class)))
        .thenThrow(givenFeignConflict());

    final ResultActions result = whenSubmitRegisterForm(validRegisterFormParams());

    result.andExpect(status().isOk())
        .andExpect(view().name("register"))
        .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));
  }

  @Test
  @DisplayName("POST /register sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute soumission du formulaire "
      + "d'inscription sans jeton CSRF valide doit être rejetée.")
  void shouldRejectRegistrationWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(REGISTER_URL)
            .param("firstName", "Sofia")
            .param("lastName", "Kovalenko")
            .param("email", "sofia@tortiki.fr")
            .param("password", "motdepasse123")
            .param("role", "SELLER"))
        .andExpect(status().isForbidden());
  }

  @Step("Préparer les paramètres valides du formulaire d'inscription")
  private String validRegisterFormParams() {
    return "firstName=Sofia&lastName=Kovalenko&email=sofia%40tortiki.fr"
        + "&password=motdepasse123&role=SELLER";
  }

  @Step("Simuler un conflit 409 (email déjà utilisé) sur tortiki-api")
  private FeignException.Conflict givenFeignConflict() {
    final Request request = Request.create(
        HttpMethod.POST, "/api/v1/auth/register", Collections.emptyMap(),
        null, StandardCharsets.UTF_8, new RequestTemplate());
    return new FeignException.Conflict(
        "Email déjà utilisé", request, null, Collections.emptyMap());
  }

  @Step("Soumettre le formulaire d'inscription avec jeton CSRF")
  private ResultActions whenSubmitRegisterForm(final String formParams) throws Exception {
    final String[] pairs = formParams.split("&");
    final var request = post(REGISTER_URL).with(csrf());
    for (final String pair : pairs) {
      final String[] keyValue = pair.split("=", 2);
      request.param(keyValue[0], java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
    }
    return mockMvc.perform(request);
  }
}