package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.dto.listing.AllergenResponse;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
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
import io.qameta.allure.Story;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests unitaires du contrôleur {@link SellerListingController}.
 *
 * <p>Utilise {@code WebMvcTest} pour charger uniquement la couche web —
 * {@link ListingApiClient} est entièrement mocké, aucun appel réel à
 * {@code tortiki-api} n'est effectué.</p>
 *
 * <p>Vérifie que le formulaire de création/édition transmet à l'API un
 * payload conforme au contrat réel ({@code portions}, {@code pickupAddress},
 * {@code pickupDatetime} — cf. correctif Issue 53), et que la désactivation
 * d'annonce ainsi que le repli sur les allergènes indisponibles fonctionnent
 * correctement.</p>
 */
@WebMvcTest(SellerListingController.class)
@ActiveProfiles("test")
@Epic("Espace vendeur")
@Feature("Gestion des annonces (Issue 53)")
@DisplayName("SellerListingController")
class SellerListingControllerTest {

  private static final String SELLER_EMAIL = "sofia@tortiki.fr";
  private static final Long LISTING_ID = 1L;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListingApiClient listingApiClient;

  private ListingDetailResponse detail;

  @BeforeEach
  void setUp() {
    detail = new ListingDetailResponse(
        LISTING_ID, "Bortsch ukrainien", "Soupe traditionnelle",
        new BigDecimal("8.50"), 4, 1L, List.of(),
        "12 rue de la Paix, 54000 Nancy",
        LocalDateTime.now().plusDays(7), null,
        "ACTIVE");

    when(listingApiClient.getCuisineTypes())
        .thenReturn(List.of(new CuisineTypeResponse(
            1L, "Ukrainienne", "Plat traditionnel ukrainienne", true)));
    when(listingApiClient.getAllergens())
        .thenReturn(List.of(new AllergenResponse(1L, "Gluten")));
  }

  @Test
  @Story("Consultation des annonces")
  @Severity(SeverityLevel.NORMAL)
  @Description("Le vendeur authentifié consulte la liste de ses annonces, tous statuts confondus.")
  @DisplayName("GET /seller/listings retourne la vue liste")
  void myListings_shouldReturnListView() throws Exception {
    when(listingApiClient.getMyListings()).thenReturn(List.of(detail));

    mockMvc.perform(get("/seller/listings").with(user(SELLER_EMAIL).roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("seller-listings"))
        .andExpect(model().attributeExists("listings"));
  }

  @Test
  @Story("Création d'annonce")
  @Severity(SeverityLevel.NORMAL)
  @Description(
      "Affiche un formulaire vide avec les listes de référence (types de cuisine, allergènes).")
  @DisplayName("GET /seller/listings/new affiche le formulaire vide")
  void newListingForm_shouldReturnFormView() throws Exception {
    mockMvc.perform(get("/seller/listings/new")
            .with(user(SELLER_EMAIL).roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("isEdit", false))
        .andExpect(model().attribute("formAction", "/seller/listings/new"));
  }

  @Test
  @Story("Création d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Un payload incomplet réaffiche le formulaire avec les erreurs sur "
      + "portions, pickupAddress et pickupDatetime — contrat aligné sur tortiki-api.")
  @DisplayName("POST /seller/listings/new avec champs manquants réaffiche le formulaire")
  void createListing_shouldReturnFormView_whenRequiredFieldsMissing() throws Exception {
    mockMvc.perform(post("/seller/listings/new")
            .with(csrf())
            .with(user(SELLER_EMAIL).roles("SELLER"))
            .param("title", "Bortsch")
            .param("price", "8.50"))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "pickupDatetime"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "portions"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "pickupAddress"));
  }

  @Test
  @Story("Création d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description(
      "Un payload valide crée l'annonce via l'API et redirige vers la liste avec un message flash."
  )
  @DisplayName("POST /seller/listings/new valide crée l'annonce et redirige")
  void createListing_shouldRedirect_whenPayloadValid() throws Exception {
    when(listingApiClient.create(any())).thenReturn(detail);

    mockMvc.perform(post("/seller/listings/new")
            .with(csrf())
            .with(user(SELLER_EMAIL).roles("SELLER"))
            .param("title", "Bortsch ukrainien")
            .param("description", "Soupe traditionnelle")
            .param("price", "8.50")
            .param("portions", "4")
            .param("pickupAddress", "12 rue de la Paix, 54000 Nancy")
            .param("pickupDatetime", LocalDateTime.now().plusDays(7).toString())
            .param("cuisineTypeId", "1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/seller/listings"))
        .andExpect(flash().attribute("success", "Annonce publiée avec succès."));

    verify(listingApiClient).create(any());
  }

  @Test
  @Story("Édition d'annonce")
  @Severity(SeverityLevel.NORMAL)
  @Description("Le formulaire d'édition est pré-rempli avec les données existantes de l'annonce.")
  @DisplayName("GET /seller/listings/{id}/edit pré-remplit le formulaire")
  void editListingForm_shouldPrefillFields() throws Exception {
    when(listingApiClient.getById(anyLong())).thenReturn(detail);

    mockMvc.perform(get("/seller/listings/1/edit").with(user(SELLER_EMAIL).roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("isEdit", true))
        .andExpect(model().attribute("formAction", "/seller/listings/1/edit"))
        .andExpect(model().attributeExists("listingRequest"));
  }

  @Test
  @Story("Édition d'annonce")
  @Severity(SeverityLevel.CRITICAL)
  @Description(
      "Une mise à jour valide appelle l'API update et redirige avec un message flash de succès.")
  @DisplayName("POST /seller/listings/{id}/edit valide met à jour et redirige")
  void updateListing_shouldRedirect_whenPayloadValid() throws Exception {
    when(listingApiClient.update(anyLong(), any())).thenReturn(detail);

    mockMvc.perform(post("/seller/listings/1/edit")
            .with(csrf())
            .with(user(SELLER_EMAIL).roles("SELLER"))
            .param("title", "Bortsch ukrainien")
            .param("description", "Soupe traditionnelle")
            .param("price", "8.50")
            .param("portions", "4")
            .param("pickupAddress", "12 rue de la Paix, 54000 Nancy")
            .param("pickupDatetime", LocalDateTime.now().plusDays(7).toString())
            .param("cuisineTypeId", "1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/seller/listings"))
        .andExpect(flash().attribute("success", "Annonce mise à jour avec succès."));

    verify(listingApiClient).update(anyLong(), any());
  }

  @Test
  @Story("Désactivation d'annonce")
  @Severity(SeverityLevel.NORMAL)
  @Description(
      "La désactivation appelle le DELETE Feign (suppression logique) "
          + "et redirige avec message flash.")
  @DisplayName("POST /seller/listings/{id}/delete désactive l'annonce et redirige")
  void deleteListing_shouldRedirect_afterDeactivation() throws Exception {
    mockMvc.perform(post("/seller/listings/1/delete")
            .with(csrf())
            .with(user(SELLER_EMAIL).roles("SELLER")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/seller/listings"))
        .andExpect(flash().attribute("success", "Annonce désactivée avec succès."));

    verify(listingApiClient).delete(LISTING_ID);
  }

  @Test
  @Story("Résilience API")
  @Severity(SeverityLevel.MINOR)
  @Description("Si l'endpoint /api/v1/allergens est indisponible (500), le formulaire "
      + "s'affiche malgré tout avec une liste d'allergènes vide (safeGetAllergens).")
  @DisplayName("GET /seller/listings/new tolère l'indisponibilité de l'endpoint allergènes")
  void newListingForm_shouldToleratesAllergensEndpointFailure() throws Exception {
    Request feignRequest = Request.create(HttpMethod.GET, "/api/v1/allergens",
        java.util.Map.of(), null, new RequestTemplate());
    when(listingApiClient.getAllergens())
        .thenThrow(new FeignException.InternalServerError(
            "erreur serveur", feignRequest, null, null));

    mockMvc.perform(get("/seller/listings/new").with(user(SELLER_EMAIL).roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attribute("allergens", List.of()));
  }
}