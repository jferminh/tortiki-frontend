package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.dto.listing.ListingCardResponse;
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
 * Tests unitaires de la couche web {@code SearchController}.
 *
 * <p>{@code SearchApiClient} est simulé via {@code @MockitoBean}. {@code
 * SecurityConfig} est importé pour vérifier les règles {@code permitAll}
 * réelles de {@code /search} et {@code /search/results}.</p>
 */
@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Recherche d'annonces")
class SearchControllerTest {

  private static final String SEARCH_URL = "/search";
  private static final String SEARCH_RESULTS_URL = "/search/results";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SearchApiClient searchApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @Test
  @DisplayName("GET /search retourne 200 avec le formulaire vide et les types de cuisine")
  @Story("Formulaire de recherche")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo (visiteur anonyme) affiche le formulaire de recherche, "
      + "sans avoir encore saisi de critères.")
  void shouldReturnSearchFormWithCuisineTypes() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    mockMvc.perform(get(SEARCH_URL))
        .andExpect(status().isOk())
        .andExpect(view().name("search"))
        .andExpect(model().attributeExists("cuisineTypes"));
  }

  @Test
  @DisplayName("GET /search/results avec critères complets retourne 200 et la grille de résultats")
  @Story("Résultats de recherche")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo recherche des plats ukrainiens à Frouard : le contrôleur "
      + "transmet les critères à SearchApiClient.search et affiche la grille.")
  void shouldReturnSearchResultsWithGivenCriteria() throws Exception {
    final List<ListingCardResponse> results = givenListingResults();
    when(searchApiClient.search("bortsch", "Frouard", "54390", 1L, 0, 12))
        .thenReturn(results);
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    final ResultActions result = whenSearchWithCriteria(
        "query=bortsch&city=Frouard&postalCode=54390&cuisineTypeId=1&page=0&size=12");

    result.andExpect(status().isOk())
        .andExpect(view().name("search-results"))
        .andExpect(model().attribute("results", results));
    verify(searchApiClient).search("bortsch", "Frouard", "54390", 1L, 0, 12);
  }

  @Test
  @DisplayName("GET /search/results sans page ni size applique les valeurs par défaut (0, 12)")
  @Story("Résultats de recherche — valeurs par défaut")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Cas d'usage le plus fréquent : un visiteur lance une recherche "
      + "simple sans préciser de pagination. Le contrôleur doit appliquer "
      + "DEFAULT_PAGE = 0 et DEFAULT_PAGE_SIZE = 12 plutôt que d'échouer en 400 "
      + "(régression corrigée : SearchCriteria.page/size étaient en int primitif).")
  void shouldApplyDefaultPageAndSizeWhenNotProvided() throws Exception {
    when(searchApiClient.search(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of());
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    whenSearchWithCriteria("query=bortsch")
        .andExpect(status().isOk());

    verify(searchApiClient).search(eq("bortsch"), any(), any(), any(), eq(0), eq(12));
  }

  @Test
  @DisplayName("GET /search/results sans résultat affiche une grille vide")
  @Story("Résultats de recherche")
  @Severity(SeverityLevel.NORMAL)
  @Description("Cas limite : aucune annonce ne correspond aux critères, la page "
      + "doit s'afficher normalement avec une liste de résultats vide.")
  void shouldReturnEmptyResultsWhenNoListingMatches() throws Exception {
    when(searchApiClient.search(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of());
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    final ResultActions result = whenSearchWithCriteria("query=introuvable");

    result.andExpect(status().isOk())
        .andExpect(view().name("search-results"))
        .andExpect(model().attribute("results", List.of()));
  }

  @Test
  @DisplayName("GET /search/results est accessible sans authentification (route permitAll)")
  @Story("Accès public")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /search/** est "
      + "déclaré permitAll, contrairement aux routes /dashboard et /admin.")
  void shouldAllowAnonymousAccessToSearchResults() throws Exception {
    when(searchApiClient.search(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of());
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    whenSearchWithCriteria("query=test").andExpect(status().isOk());
  }

  @Step("Préparer deux résultats d'annonces")
  private List<ListingCardResponse> givenListingResults() {
    return List.of(
        new ListingCardResponse(1L, "Bortsch ukrainien", "Soupe traditionnelle",
            new BigDecimal("8.50"), 4, "https://minio/bortsch.jpg",
            "12 rue de la Paix, Frouard", "Ukrainienne", "Sofia", "Frouard"),
        new ListingCardResponse(2L, "Varenyky", "Raviolis ukrainiens",
            new BigDecimal("6.00"), 6, "https://minio/varenyky.jpg",
            "12 rue de la Paix, Frouard", "Ukrainienne", "Sofia", "Frouard"));
  }

  @Step("GET /search/results avec les critères : {queryParams}")
  private ResultActions whenSearchWithCriteria(final String queryParams) throws Exception {
    return mockMvc.perform(get(SEARCH_RESULTS_URL + "?" + queryParams));
  }
}