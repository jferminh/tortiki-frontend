package com.tortiki.frontend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.AllergenApiClient;
import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.client.ReviewApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.config.security.ApiLogoutHandler;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import com.tortiki.frontend.dto.listing.AllergenResponse;
import com.tortiki.frontend.dto.listing.ListingDetailResponse;
import com.tortiki.frontend.dto.listing.ListingSummaryResponse;
import com.tortiki.frontend.dto.review.ReviewResponse;
import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * Tests unitaires de la couche web {@code ListingController}.
 *
 * <p>{@code ListingApiClient}, {@code ReviewApiClient} et
 * {@code AllergenApiClient} sont simulés via {@code @MockitoBean}.
 * {@code SecurityConfig} est importé pour vérifier que {@code /listings/**}
 * est bien déclaré {@code permitAll()} — Théo doit pouvoir consulter une
 * fiche plat avant de se connecter.</p>
 */
@WebMvcTest(ListingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Consultation d'annonces")
class ListingControllerTest {

  private static final String LISTING_DETAIL_URL = "/listings/1";
  private static final String LISTINGS_URL = "/listings";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListingApiClient listingApiClient;

  @MockitoBean
  private ReviewApiClient reviewApiClient;

  @MockitoBean
  private AllergenApiClient allergenApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @MockitoBean
  private ApiLogoutHandler apiLogoutHandler;

  @Test
  @DisplayName("GET /listings/{id} retourne 200 avec l'annonce et ses avis")
  @Story("Consultation publique d'une fiche plat")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo (visiteur anonyme) consulte la fiche du bortsch de Sofia : "
      + "le modèle doit contenir l'annonce (ListingApiClient.findById) et ses "
      + "avis (ReviewApiClient.findByListingId).")
  void shouldReturnListingDetailWithReviews() throws Exception {
    final ListingDetailResponse listing = givenActiveListing();
    final List<ReviewResponse> reviews = givenReviews();
    when(listingApiClient.findById(1L)).thenReturn(listing);
    when(reviewApiClient.findByListingId(1L)).thenReturn(reviews);

    final ResultActions result = whenViewListingDetail();

    result.andExpect(status().isOk())
        .andExpect(view().name("listing-detail"))
        .andExpect(model().attribute("listing", listing))
        .andExpect(model().attribute("reviews", reviews));
  }

  @Test
  @DisplayName("GET /listings/{id} pré-remplit un formulaire de contact vide")
  @Story("Consultation publique d'une fiche plat")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Le formulaire de contact affiché sur la fiche doit être "
      + "pré-rempli avec le listingId cible, message et portions à null, "
      + "conformément au comportement documenté du contrôleur.")
  void shouldPrefillEmptyContactRequestWithListingId() throws Exception {
    when(listingApiClient.findById(1L)).thenReturn(givenActiveListing());
    when(reviewApiClient.findByListingId(1L)).thenReturn(List.of());

    final ResultActions result = whenViewListingDetail();

    result.andExpect(status().isOk())
        .andExpect(model().attribute("contactRequest",
            new CreateContactRequestRequest(1L, null, null)));
  }

  @Test
  @DisplayName("GET /listings/{id} sans avis affiche une liste vide")
  @Story("Consultation publique d'une fiche plat")
  @Severity(SeverityLevel.NORMAL)
  @Description("Cas limite : l'annonce n'a encore reçu aucune évaluation, "
      + "la fiche doit tout de même s'afficher normalement.")
  void shouldReturnListingDetailWithEmptyReviewsWhenNoneExist() throws Exception {
    when(listingApiClient.findById(1L)).thenReturn(givenActiveListing());
    when(reviewApiClient.findByListingId(1L)).thenReturn(List.of());

    final ResultActions result = whenViewListingDetail();

    result.andExpect(status().isOk())
        .andExpect(model().attribute("reviews", List.of()));
  }

  @Test
  @DisplayName("GET /listings/{id} est accessible sans authentification (route permitAll)")
  @Story("Accès public")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /listings/** "
      + "est déclaré permitAll, contrairement aux routes /dashboard et /admin.")
  void shouldAllowAnonymousAccessToListingDetail() throws Exception {
    when(listingApiClient.findById(1L)).thenReturn(givenActiveListing());
    when(reviewApiClient.findByListingId(1L)).thenReturn(List.of());

    whenViewListingDetail().andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /listings/{id} résout les noms d'allergènes depuis leurs identifiants")
  @Story("Consultation publique d'une fiche plat")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Le bortsch de Sofia contient gluten (id 1) et œufs (id 3) : le "
      + "modèle doit exposer allergenNames = [\"Gluten\", \"Œufs\"], résolu via "
      + "AllergenApiClient.getAllergens à partir des allergenIds de l'annonce.")
  void shouldResolveAllergenNamesFromIds() throws Exception {
    when(listingApiClient.findById(1L)).thenReturn(givenListingWithAllergenIds(List.of(1L, 3L)));
    when(reviewApiClient.findByListingId(1L)).thenReturn(List.of());
    when(allergenApiClient.getAllergens()).thenReturn(givenAllAllergens());

    final ResultActions result = whenViewListingDetail();

    result.andExpect(status().isOk())
        .andExpect(model().attribute("allergenNames", List.of("Gluten", "Œufs")));
  }

  @Test
  @DisplayName("GET /listings/{id} affiche la fiche sans allergènes si le service est indisponible")
  @Story("Consultation publique d'une fiche plat")
  @Severity(SeverityLevel.NORMAL)
  @Description("Dégradation progressive : si AllergenApiClient.getAllergens échoue "
      + "(panne réseau, service down), la fiche doit tout de même s'afficher avec "
      + "allergenNames vide plutôt que de renvoyer une erreur 500.")
  void shouldReturnEmptyAllergenNamesWhenAllergenServiceUnavailable() throws Exception {
    when(listingApiClient.findById(1L)).thenReturn(givenListingWithAllergenIds(List.of(1L)));
    when(reviewApiClient.findByListingId(1L)).thenReturn(List.of());
    when(allergenApiClient.getAllergens()).thenThrow(givenAllergenServiceUnavailable());

    final ResultActions result = whenViewListingDetail();

    result.andExpect(status().isOk())
        .andExpect(model().attribute("allergenNames", List.of()));
  }

  @Test
  @DisplayName("GET /listings retourne 200 avec la liste des annonces actives")
  @Story("Liste publique des plats disponibles")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Un visiteur anonyme consulte /listings : le modèle doit contenir "
      + "la liste complète des annonces actives retournée par ListingApiClient.findAll.")
  void shouldReturnListingsWithAllActiveAnnonces() throws Exception {
    final List<ListingSummaryResponse> listings = givenActiveListingSummaries();
    when(listingApiClient.findAll()).thenReturn(listings);

    final ResultActions result = whenViewListings();

    result.andExpect(status().isOk())
        .andExpect(view().name("listings"))
        .andExpect(model().attribute("listings", listings));
  }

  @Test
  @DisplayName("GET /listings sans annonce active affiche une liste vide")
  @Story("Liste publique des plats disponibles")
  @Severity(SeverityLevel.NORMAL)
  @Description("Cas limite : aucune annonce active n'existe encore, la vue doit "
      + "tout de même s'afficher normalement avec l'état vide.")
  void shouldReturnEmptyListingsWhenNoneActiveExist() throws Exception {
    when(listingApiClient.findAll()).thenReturn(List.of());

    final ResultActions result = whenViewListings();

    result.andExpect(status().isOk())
        .andExpect(model().attribute("listings", List.of()));
  }

  @Test
  @DisplayName("GET /listings est accessible sans authentification (route permitAll)")
  @Story("Accès public")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /listings/** est "
      + "déclaré permitAll, cohérent avec /listings/{id} déjà testé.")
  void shouldAllowAnonymousAccessToListings() throws Exception {
    when(listingApiClient.findAll()).thenReturn(List.of());

    whenViewListings().andExpect(status().isOk());
  }

  @Step("Préparer une annonce active de Sofia sans allergène")
  private ListingDetailResponse givenActiveListing() {
    return givenListingWithAllergenIds(List.of());
  }

  @Step("Préparer une annonce active de Sofia avec des allergènes donnés")
  private ListingDetailResponse givenListingWithAllergenIds(final List<Long> allergenIds) {
    return new ListingDetailResponse(
        1L, "Bortsch ukrainien", "Soupe traditionnelle mijotée",
        new BigDecimal("8.50"), 4, 1L, allergenIds,
        "12 rue de la Paix, Frouard", LocalDateTime.now().plusDays(1),
        "https://minio/bortsch.jpg", "ACTIVE");
  }

  @Step("Préparer le référentiel complet des allergènes")
  private List<AllergenResponse> givenAllAllergens() {
    return List.of(
        new AllergenResponse(1L, "Gluten"),
        new AllergenResponse(2L, "Lactose"),
        new AllergenResponse(3L, "Œufs"));
  }

  @Step("Simuler une panne du service allergènes")
  private FeignException givenAllergenServiceUnavailable() {
    final Request request = Request.create(HttpMethod.GET, "/api/v1/allergens",
        Map.of(), null, null, null);
    return new FeignException.InternalServerError("Service indisponible", request, null, null);
  }

  @Step("Préparer deux avis sur l'annonce")
  private List<ReviewResponse> givenReviews() {
    return List.of(
        new ReviewResponse(1L, 1L, "Théo", 5, "Excellent !", LocalDateTime.now()),
        new ReviewResponse(2L, 1L, "Léa", 4, "Très bon", LocalDateTime.now()));
  }

  @Step("GET /listings/1 sans authentification")
  private ResultActions whenViewListingDetail() throws Exception {
    return mockMvc.perform(get(LISTING_DETAIL_URL));
  }

  @Step("Préparer deux annonces actives au format carte")
  private List<ListingSummaryResponse> givenActiveListingSummaries() {
    return List.of(
        new ListingSummaryResponse(1L, "Bortsch ukrainien",
            "Soupe traditionnelle mijotée", new BigDecimal("8.50"), 4,
            "https://minio/bortsch.jpg", "12 rue de la Paix, Frouard",
            "Ukrainienne", "sofia@tortiki.fr"),
        new ListingSummaryResponse(2L, "Varenyky",
            "Raviolis ukrainiens fait maison", new BigDecimal("6.00"), 6,
            "https://minio/varenyky.jpg", "12 rue de la Paix, Frouard",
            "Ukrainienne", "sofia@tortiki.fr")
    );
  }

  @Step("GET /listings sans authentification")
  private ResultActions whenViewListings() throws Exception {
    return mockMvc.perform(get(LISTINGS_URL));
  }
}