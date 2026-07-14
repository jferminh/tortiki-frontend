package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.config.security.ApiLogoutHandler;
import com.tortiki.frontend.dto.listing.CreateListingRequest;
import com.tortiki.frontend.dto.listing.ListingDetailResponse;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests unitaires de la couche web {@code SellerListingController}.
 *
 * <p>{@code ListingApiClient} est simulé via {@code @MockitoBean}.
 * {@code SecurityConfig} est importé pour vérifier que {@code
 * /seller/listings/**} exige bien une authentification (SELLER_ROUTES),
 * l'authentification étant simulée via {@code user().roles("SELLER")}
 * puisque {@code ApiDelegatingAuthenticationProvider} est mocké.</p>
 *
 * <p>{@code ApiLogoutHandler} est également simulé : {@code @WebMvcTest}
 * ne scanne pas les {@code @Component} hors couche web, alors que {@code
 * SecurityConfig#securityFilterChain} en dépend désormais pour le logout
 * délégué à l'API. Sans ce mock, le contexte Spring ne démarre pas.</p>
 */
@WebMvcTest(SellerListingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Gestion des annonces vendeur")
class SellerListingControllerTest {

  private static final String SELLER_LISTINGS_URL = "/seller/listings";
  private static final String SELLER_LISTINGS_NEW_URL = "/seller/listings/new";
  private static final String SELLER_EMAIL = "sofia@tortiki.fr";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListingApiClient listingApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @MockitoBean
  private ApiLogoutHandler apiLogoutHandler;

  @Test
  @DisplayName("GET /seller/listings sans authentification redirige vers /login")
  @Story("Accès restreint")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /seller/** "
      + "fait partie de SELLER_ROUTES, un visiteur anonyme doit être redirigé.")
  void shouldRedirectToLoginWhenAnonymousAccessesMyListings() throws Exception {
    mockMvc.perform(get(SELLER_LISTINGS_URL))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("GET /seller/listings authentifié retourne 200 avec les annonces du vendeur")
  @Story("Consultation des annonces vendeur")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia consulte ses annonces, tous statuts confondus, résolues "
      + "côté API depuis le cookie de session (aucun email transmis en paramètre).")
  void shouldReturnMyListingsForAuthenticatedSeller() throws Exception {
    final List<ListingDetailResponse> listings = List.of(givenActiveListing());
    when(listingApiClient.getMyListings()).thenReturn(listings);

    mockMvc.perform(get(SELLER_LISTINGS_URL).with(sellerUser()))
        .andExpect(status().isOk())
        .andExpect(view().name("seller-listings"))
        .andExpect(model().attribute("listings", listings));
  }

  @Test
  @DisplayName("GET /seller/listings/new retourne 200 avec un formulaire de création vide")
  @Story("Création d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia affiche le formulaire vide de création, avec isEdit=false "
      + "et l'action pointant vers /seller/listings/new.")
  void shouldReturnEmptyCreateForm() throws Exception {
    when(listingApiClient.getCuisineTypes()).thenReturn(List.of());
    when(listingApiClient.getAllergens()).thenReturn(List.of());

    mockMvc.perform(get(SELLER_LISTINGS_NEW_URL).with(sellerUser()))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("isEdit", false))
        .andExpect(model().attribute("formAction", SELLER_LISTINGS_NEW_URL))
        .andExpect(model().attribute("listingRequest",
            new CreateListingRequest(null, null, null, null, null, null, null, null)));
  }

  @Test
  @DisplayName("GET /seller/listings/new tolère une panne de l'endpoint allergènes")
  @Story("Création d'annonce — résilience")
  @Severity(SeverityLevel.NORMAL)
  @Description("Si /api/v1/allergens répond en erreur 500 (endpoint en cours de "
      + "stabilisation côté API), le formulaire doit tout de même s'afficher "
      + "avec une liste d'allergènes vide plutôt que de propager l'erreur.")
  void shouldDegradeGracefullyWhenAllergensEndpointFails() throws Exception {
    when(listingApiClient.getCuisineTypes()).thenReturn(List.of());
    when(listingApiClient.getAllergens()).thenThrow(givenFeignServerError());

    mockMvc.perform(get(SELLER_LISTINGS_NEW_URL).with(sellerUser()))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("allergens", List.of()));
  }

  @Test
  @DisplayName("POST /seller/listings/new avec données valides sans photo redirige")
  @Story("Création d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia crée une annonce de bortsch sans téléverser de photo : "
      + "délégation à ListingApiClient.create, aucun appel à uploadPhoto.")
  void shouldCreateListingWithoutPhotoAndRedirect() throws Exception {
    when(listingApiClient.create(any(CreateListingRequest.class)))
        .thenReturn(givenActiveListing());

    final ResultActions result = whenSubmitCreateForm();

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(SELLER_LISTINGS_URL))
        .andExpect(flash().attributeExists("success"));
    verify(listingApiClient).create(any(CreateListingRequest.class));
    verify(listingApiClient, never()).uploadPhoto(any(), any());
  }

  @Test
  @DisplayName("POST /seller/listings/new avec photo déclenche l'upload")
  @Story("Création d'annonce")
  @Severity(SeverityLevel.NORMAL)
  @Description("Sofia crée une annonce avec une photo jointe : le contrôleur "
      + "doit déléguer à ListingApiClient.uploadPhoto après la création.")
  void shouldUploadPhotoAfterListingCreation() throws Exception {
    when(listingApiClient.create(any(CreateListingRequest.class)))
        .thenReturn(givenActiveListing());
    final MockMultipartFile photo = new MockMultipartFile(
        "photo", "bortsch.jpg", "image/jpeg", "contenu-image".getBytes(StandardCharsets.UTF_8));

    final ResultActions result = mockMvc.perform(multipart(SELLER_LISTINGS_NEW_URL)
        .file(photo)
        .param("title", "Bortsch ukrainien")
        .param("description", "Soupe traditionnelle mijotée")
        .param("price", "8.50")
        .param("portions", "4")
        .param("pickupAddress", "12 rue de la Paix, Frouard")
        .param("pickupDatetime", LocalDateTime.now().plusDays(1).toString())
        .param("cuisineTypeId", "1")
        .with(sellerUser())
        .with(csrf()));

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(SELLER_LISTINGS_URL));
    verify(listingApiClient).uploadPhoto(eq(1L), any());
  }

  @Test
  @DisplayName("POST /seller/listings/new avec titre vide réaffiche le formulaire")
  @Story("Création d'annonce — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un titre vide doit être rejeté par Bean Validation (@NotBlank) "
      + "avant tout appel à ListingApiClient.create, avec réaffichage inline.")
  void shouldRedisplayFormWhenTitleIsBlank() throws Exception {
    when(listingApiClient.getCuisineTypes()).thenReturn(List.of());
    when(listingApiClient.getAllergens()).thenReturn(List.of());

    final ResultActions result = mockMvc.perform(post(SELLER_LISTINGS_NEW_URL)
        .with(sellerUser())
        .with(csrf())
        .param("title", "")
        .param("description", "Soupe traditionnelle mijotée")
        .param("price", "8.50")
        .param("portions", "4")
        .param("pickupAddress", "12 rue de la Paix, Frouard")
        .param("pickupDatetime", LocalDateTime.now().plusDays(1).toString())
        .param("cuisineTypeId", "1"));

    result.andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "title"));
    verify(listingApiClient, never()).create(any());
  }

  @Test
  @DisplayName("GET /seller/listings/{id}/edit retourne 200 avec le formulaire pré-rempli")
  @Story("Édition d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia édite son annonce existante : le formulaire doit être "
      + "pré-rempli avec les données actuelles, isEdit=true.")
  void shouldReturnPrefilledEditForm() throws Exception {
    final ListingDetailResponse listing = givenActiveListing();
    when(listingApiClient.findById(1L)).thenReturn(listing);
    when(listingApiClient.getCuisineTypes()).thenReturn(List.of());
    when(listingApiClient.getAllergens()).thenReturn(List.of());

    mockMvc.perform(get(SELLER_LISTINGS_URL + "/1/edit").with(sellerUser()))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("isEdit", true))
        .andExpect(model().attribute("formAction", SELLER_LISTINGS_URL + "/1/edit"))
        .andExpect(model().attribute("listingRequest", new CreateListingRequest(
            listing.title(), listing.description(), listing.price(), listing.portions(),
            listing.pickupAddress(), listing.pickupDatetime(), listing.cuisineTypeId(),
            listing.allergenIds())));
  }

  @Test
  @DisplayName("POST /seller/listings/{id}/edit avec données valides redirige")
  @Story("Édition d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia met à jour son annonce : délégation à ListingApiClient.update "
      + "puis redirection avec message flash de confirmation.")
  void shouldUpdateListingAndRedirect() throws Exception {
    final ResultActions result = mockMvc.perform(post(SELLER_LISTINGS_URL + "/1/edit")
        .with(sellerUser())
        .with(csrf())
        .param("title", "Bortsch ukrainien (mis à jour)")
        .param("description", "Soupe traditionnelle mijotée")
        .param("price", "9.00")
        .param("portions", "3")
        .param("pickupAddress", "12 rue de la Paix, Frouard")
        .param("pickupDatetime", LocalDateTime.now().plusDays(2).toString())
        .param("cuisineTypeId", "1"));

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(SELLER_LISTINGS_URL))
        .andExpect(flash().attributeExists("success"));
    verify(listingApiClient).update(eq(1L), any(CreateListingRequest.class));
  }

  @Test
  @DisplayName("POST /seller/listings/{id}/delete désactive l'annonce et redirige")
  @Story("Désactivation d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia désactive son annonce (suppression logique) : délégation "
      + "à ListingApiClient.delete puis redirection avec message flash.")
  void shouldDeleteListingAndRedirect() throws Exception {
    final ResultActions result = mockMvc.perform(
        post(SELLER_LISTINGS_URL + "/1/delete")
            .with(sellerUser())
            .with(csrf()));

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(SELLER_LISTINGS_URL))
        .andExpect(flash().attributeExists("success"));
    verify(listingApiClient).delete(1L);
  }

  @Test
  @DisplayName("POST /seller/listings/{id}/delete sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute désactivation d'annonce "
      + "sans jeton CSRF valide doit être rejetée, y compris pour un vendeur "
      + "authentifié.")
  void shouldRejectDeleteWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(SELLER_LISTINGS_URL + "/1/delete").with(sellerUser()))
        .andExpect(status().isForbidden());
    verify(listingApiClient, never()).delete(any());
  }

  @Step("Simuler un utilisateur vendeur authentifié")
  private org.springframework.test.web.servlet.request.RequestPostProcessor sellerUser() {
    return user(SELLER_EMAIL).roles("SELLER");
  }

  @Step("Préparer une annonce active de Sofia")
  private ListingDetailResponse givenActiveListing() {
    return new ListingDetailResponse(
        1L, "Bortsch ukrainien", "Soupe traditionnelle mijotée",
        new BigDecimal("8.50"), 4, 1L, List.of(),
        "12 rue de la Paix, Frouard", LocalDateTime.now().plusDays(1),
        "https://minio/bortsch.jpg", "ACTIVE");
  }

  @Step("Simuler une panne 500 de l'endpoint allergènes")
  private FeignException givenFeignServerError() {
    final Request request = Request.create(
        HttpMethod.GET, "/api/v1/allergens", Collections.emptyMap(),
        null, StandardCharsets.UTF_8, new RequestTemplate());
    return new FeignException.InternalServerError(
        "Service indisponible", request, null, Collections.emptyMap());
  }

  @Step("Soumettre le formulaire de création sans photo")
  private ResultActions whenSubmitCreateForm() throws Exception {
    return mockMvc.perform(post(SELLER_LISTINGS_NEW_URL)
        .with(sellerUser())
        .with(csrf())
        .param("title", "Bortsch ukrainien")
        .param("description", "Soupe traditionnelle mijotée")
        .param("price", "8.50")
        .param("portions", "4")
        .param("pickupAddress", "12 rue de la Paix, Frouard")
        .param("pickupDatetime", LocalDateTime.now().plusDays(1).toString())
        .param("cuisineTypeId", "1"));
  }
}