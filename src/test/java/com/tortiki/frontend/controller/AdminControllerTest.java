package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.eq;
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

import com.tortiki.frontend.client.AdminApiClient;
import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.dto.admin.AdminListingSummaryResponse;
import com.tortiki.frontend.dto.admin.UpdateListingStatusRequest;
import com.tortiki.frontend.dto.listing.CreateCuisineTypeRequest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.math.BigDecimal;
import java.util.List;
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
 * Tests unitaires de la couche web {@code AdminController}.
 *
 * <p>{@code AdminApiClient} et {@code SearchApiClient} sont simulés via
 * {@code @MockitoBean}. {@code SecurityConfig} est importé pour vérifier
 * que {@code /admin/**} exige bien une authentification (contrairement
 * à {@code /search} ou {@code /}), conformément à {@code SELLER_ROUTES}
 * et {@code ADMIN_ROUTES} de la configuration réelle.</p>
 *
 * <p>Le contrôle fin du rôle {@code ROLE_ADMIN} est délégué à
 * {@code tortiki-api} (403 si un non-admin authentifié appelle ces routes) ;
 * ce test ne vérifie donc que l'authentification, pas l'autorisation par rôle,
 * conformément à la Javadoc de {@code SecurityConfig}.</p>
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Panel d'administration")
class AdminControllerTest {

  private static final String ADMIN_URL = "/admin";
  private static final String ADMIN_LISTINGS_URL = "/admin/listings";
  private static final String ADMIN_CUISINE_TYPES_URL = "/admin/cuisine-types";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminApiClient adminApiClient;

  @MockitoBean
  private SearchApiClient searchApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @Test
  @DisplayName("GET /admin sans authentification redirige vers /login")
  @Story("Accès restreint")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /admin/** "
      + "n'est pas dans PUBLIC_ROUTES, un visiteur anonyme doit être redirigé.")
  void shouldRedirectToLoginWhenAnonymousAccessesDashboard() throws Exception {
    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("GET /admin authentifié retourne 200 avec annonces et types de cuisine")
  @Story("Tableau de bord admin")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Un administrateur authentifié charge le dashboard : le modèle "
      + "doit contenir les annonces (AdminApiClient) et les types de cuisine "
      + "(SearchApiClient), illustrant la séparation lecture/écriture des ports.")
  void shouldReturnDashboardForAuthenticatedAdmin() throws Exception {
    final List<AdminListingSummaryResponse> listings = givenAdminListings();
    when(adminApiClient.getAllListings()).thenReturn(listings);
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    mockMvc.perform(get(ADMIN_URL).with(user("admin@tortiki.fr").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-dashboard"))
        .andExpect(model().attribute("listings", listings));
  }

  @Test
  @DisplayName("GET /admin/listings authentifié affiche la modération des annonces")
  @Story("Modération des annonces")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Un administrateur consulte toutes les annonces de la plateforme, "
      + "tous vendeurs et tous statuts confondus, pour modération.")
  void shouldReturnListingsForModeration() throws Exception {
    when(adminApiClient.getAllListings()).thenReturn(givenAdminListings());

    mockMvc.perform(get(ADMIN_LISTINGS_URL).with(user("admin@tortiki.fr").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-listings"))
        .andExpect(model().attributeExists("listings"));
  }

  @Test
  @DisplayName("POST /admin/listings/{id}/status met à jour le statut et redirige")
  @Story("Modération des annonces")
  @Severity(SeverityLevel.CRITICAL)
  @Description("L'administrateur désactive l'annonce 1 de Sofia : le contrôleur "
      + "délègue à AdminApiClient.updateListingStatus puis redirige avec un "
      + "message flash de confirmation.")
  void shouldUpdateListingStatusAndRedirect() throws Exception {
    final ResultActions result = whenUpdateListingStatus(1L, "INACTIVE");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ADMIN_LISTINGS_URL))
        .andExpect(flash().attributeExists("success"));
    verify(adminApiClient).updateListingStatus(
        eq(1L), eq(new UpdateListingStatusRequest("INACTIVE")));
  }

  @Test
  @DisplayName("GET /admin/cuisine-types authentifié affiche le référentiel")
  @Story("Gestion du référentiel cuisine")
  @Severity(SeverityLevel.NORMAL)
  @Description("L'administrateur affiche le référentiel avec un formulaire de "
      + "création vide pré-rempli (name/description vides).")
  void shouldReturnCuisineTypesReferentialWithEmptyForm() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    mockMvc.perform(get(ADMIN_CUISINE_TYPES_URL).with(user("admin@tortiki.fr").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-cuisine-types"))
        .andExpect(model().attribute("createCuisineTypeRequest",
            new CreateCuisineTypeRequest("", "")));
  }

  @Test
  @DisplayName("POST /admin/cuisine-types avec données valides crée le type et redirige")
  @Story("Gestion du référentiel cuisine")
  @Severity(SeverityLevel.CRITICAL)
  @Description("L'administrateur crée un type de cuisine 'Maghrébine' valide : "
      + "délégation à AdminApiClient.createCuisineType puis redirection avec "
      + "message flash de confirmation.")
  void shouldCreateCuisineTypeAndRedirect() throws Exception {
    final ResultActions result = whenCreateCuisineType("Maghrébine", "Cuisine du Maghreb");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ADMIN_CUISINE_TYPES_URL))
        .andExpect(flash().attributeExists("success"));
    verify(adminApiClient).createCuisineType(
        new CreateCuisineTypeRequest("Maghrébine", "Cuisine du Maghreb"));
  }

  @Test
  @DisplayName("POST /admin/cuisine-types avec nom vide réaffiche le formulaire")
  @Story("Gestion du référentiel cuisine — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un nom vide doit être rejeté par Bean Validation (@NotBlank) "
      + "avant tout appel à AdminApiClient, avec réaffichage inline des erreurs.")
  void shouldRedisplayFormWhenNameIsBlank() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    final ResultActions result = whenCreateCuisineType("", "Description valide");

    result.andExpect(status().isOk())
        .andExpect(view().name("admin-cuisine-types"))
        .andExpect(model().attributeHasFieldErrors("createCuisineTypeRequest", "name"));
  }

  @Test
  @DisplayName("POST /admin/cuisine-types/{id}/delete supprime et redirige")
  @Story("Gestion du référentiel cuisine")
  @Severity(SeverityLevel.NORMAL)
  @Description("L'administrateur supprime le type de cuisine 3 : délégation à "
      + "AdminApiClient.deleteCuisineType puis redirection avec message flash.")
  void shouldDeleteCuisineTypeAndRedirect() throws Exception {
    final ResultActions result = mockMvc.perform(
        post(ADMIN_CUISINE_TYPES_URL + "/3/delete")
            .with(user("admin@tortiki.fr").roles("ADMIN"))
            .with(csrf()));

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ADMIN_CUISINE_TYPES_URL))
        .andExpect(flash().attributeExists("success"));
    verify(adminApiClient).deleteCuisineType(3L);
  }

  @Test
  @DisplayName("POST /admin/listings/{id}/status sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute action de modération "
      + "sans jeton CSRF valide doit être rejetée, y compris pour un admin "
      + "authentifié.")
  void shouldRejectStatusUpdateWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(ADMIN_LISTINGS_URL + "/1/status")
            .with(user("admin@tortiki.fr").roles("ADMIN"))
            .param("newStatus", "INACTIVE"))
        .andExpect(status().isForbidden());
  }

  @Step("Préparer deux résumés d'annonces pour la modération")
  private List<AdminListingSummaryResponse> givenAdminListings() {
    return List.of(
        new AdminListingSummaryResponse(1L, "Bortsch ukrainien", "sofia@tortiki.fr",
            "Frouard", new BigDecimal("8.50"), "ACTIVE", "https://minio/bortsch.jpg"),
        new AdminListingSummaryResponse(2L, "Varenyky", "sofia@tortiki.fr",
            "Frouard", new BigDecimal("6.00"), "INACTIVE", "https://minio/varenyky.jpg"));
  }

  @Step("PATCH le statut de l'annonce {id} vers {newStatus}")
  private ResultActions whenUpdateListingStatus(final Long id, final String newStatus)
      throws Exception {
    return mockMvc.perform(post(ADMIN_LISTINGS_URL + "/" + id + "/status")
        .with(user("admin@tortiki.fr").roles("ADMIN"))
        .with(csrf())
        .param("newStatus", newStatus));
  }

  @Step("Soumettre le formulaire de création de type de cuisine")
  private ResultActions whenCreateCuisineType(final String name, final String description)
      throws Exception {
    return mockMvc.perform(post(ADMIN_CUISINE_TYPES_URL)
        .with(user("admin@tortiki.fr").roles("ADMIN"))
        .with(csrf())
        .param("name", name)
        .param("description", description));
  }
}