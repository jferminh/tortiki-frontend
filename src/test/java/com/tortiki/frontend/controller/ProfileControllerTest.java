package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.UserApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.config.security.ApiLogoutHandler;
import com.tortiki.frontend.dto.user.UpdateUserProfileRequest;
import com.tortiki.frontend.dto.user.UserResponse;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.Set;
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
 * Tests unitaires de la couche web {@code ProfileController}.
 *
 * <p>{@code UserApiClient} est simulé via {@code @MockitoBean}.
 * {@code SecurityConfig} est importé pour vérifier que {@code /profile}
 * exige bien une authentification, cette route n'étant pas déclarée
 * dans {@code PUBLIC_ROUTES}.</p>
 *
 * <p>{@code ApiLogoutHandler} est également simulé : {@code @WebMvcTest}
 * ne scanne pas les {@code @Component} hors couche web, alors que {@code
 * SecurityConfig#securityFilterChain} en dépend désormais pour le logout
 * délégué à l'API. Sans ce mock, le contexte Spring ne démarre pas.</p>
 */
@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Profil utilisateur")
class ProfileControllerTest {

  private static final String PROFILE_URL = "/profile";
  private static final String PROFILE_UPDATE_URL = "/profile/update";
  private static final String USER_EMAIL = "theo@tortiki.fr";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserApiClient userApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @MockitoBean
  private ApiLogoutHandler apiLogoutHandler;

  @Test
  @DisplayName("GET /profile sans authentification redirige vers /login")
  @Story("Accès restreint")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /profile "
      + "n'est pas dans PUBLIC_ROUTES, un visiteur anonyme doit être redirigé.")
  void shouldRedirectToLoginWhenAnonymousAccessesProfile() throws Exception {
    mockMvc.perform(get(PROFILE_URL))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("GET /profile authentifié retourne 200 avec le profil pré-rempli")
  @Story("Consultation du profil")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo consulte son profil : le modèle doit contenir le profil "
      + "actuel et un formulaire de mise à jour pré-rempli avec ses prénom/nom.")
  void shouldReturnProfileWithPrefilledForm() throws Exception {
    final UserResponse profile = givenBuyerProfile();
    when(userApiClient.getMyProfile()).thenReturn(profile);

    mockMvc.perform(get(PROFILE_URL).with(buyerUser()))
        .andExpect(status().isOk())
        .andExpect(view().name("profile"))
        .andExpect(model().attribute("profile", profile))
        .andExpect(model().attribute("updateUserProfileRequest",
            new UpdateUserProfileRequest("Théo", "Dupont")));
  }

  @Test
  @DisplayName("POST /profile/update avec données valides redirige avec succès")
  @Story("Mise à jour du profil")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo met à jour son prénom : délégation à "
      + "UserApiClient.updateMyProfile puis redirection avec message flash.")
  void shouldUpdateProfileAndRedirect() throws Exception {
    final ResultActions result = whenUpdateProfile("Théodore");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PROFILE_URL))
        .andExpect(flash().attributeExists("success"));
    verify(userApiClient).updateMyProfile(
        new UpdateUserProfileRequest("Théodore", "Dupont"));
  }

  @Test
  @DisplayName("POST /profile/update avec prénom vide redirige sans appel API")
  @Story("Mise à jour du profil — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un prénom vide doit être rejeté par Bean Validation (@NotBlank) "
      + "avant tout appel à UserApiClient.updateMyProfile, avec les erreurs "
      + "exposées en attribut flash BindingResult.")
  void shouldRedirectWithValidationErrorsWhenFirstNameIsBlank() throws Exception {
    final ResultActions result = whenUpdateProfile("");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(PROFILE_URL))
        .andExpect(flash().attributeExists(
            "org.springframework.validation.BindingResult.updateUserProfileRequest"));
    verify(userApiClient, never()).updateMyProfile(any());
  }

  @Test
  @DisplayName("POST /profile/update sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute mise à jour de profil "
      + "sans jeton CSRF valide doit être rejetée, y compris pour un "
      + "utilisateur authentifié.")
  void shouldRejectUpdateWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(PROFILE_UPDATE_URL)
            .with(buyerUser())
            .param("firstName", "Théodore")
            .param("lastName", "Dupont"))
        .andExpect(status().isForbidden());
    verify(userApiClient, never()).updateMyProfile(any());
  }

  @Step("Simuler un utilisateur acheteur authentifié")
  private org.springframework.test.web.servlet.request.RequestPostProcessor buyerUser() {
    return user(USER_EMAIL).roles("BUYER");
  }

  @Step("Préparer le profil de Théo")
  private UserResponse givenBuyerProfile() {
    return new UserResponse(1L, USER_EMAIL, "Théo", "Dupont", Set.of("BUYER"));
  }

  @Step("Soumettre la mise à jour du profil avec firstName={firstName}, lastName fixe=Dupont")
  private ResultActions whenUpdateProfile(final String firstName) throws Exception {
    return mockMvc.perform(post(PROFILE_UPDATE_URL)
        .with(buyerUser())
        .with(csrf())
        .param("firstName", firstName)
        .param("lastName", "Dupont"));
  }
}