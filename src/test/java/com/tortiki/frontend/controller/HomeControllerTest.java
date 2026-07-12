package com.tortiki.frontend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.SearchApiClient;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Tests unitaires de la couche web {@code HomeController}.
 *
 * <p>{@code SearchApiClient} est simulé via {@code @MockitoBean} — le
 * routage, le modèle Thymeleaf et la dégradation gracieuse en cas de
 * panne distante sont vérifiés, jamais l'appel HTTP réel vers
 * {@code tortiki-api}.</p>
 */
@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Page d'accueil")
class HomeControllerTest {

  private static final String HOME_URL = "/";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SearchApiClient searchApiClient;

  @Test
  @DisplayName("GET / retourne 200 et affiche les types de cuisine disponibles")
  @Story("Navigation par type de cuisine")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo (visiteur non authentifié) charge la page d'accueil et voit "
      + "les cards de types de cuisine, rendues à partir de SearchApiClient.getCuisineTypes.")
  void shouldReturnHomeWithCuisineTypes() throws Exception {
    final List<CuisineTypeResponse> cuisineTypes = givenActiveCuisineTypes();
    when(searchApiClient.getCuisineTypes()).thenReturn(cuisineTypes);

    final ResultActions result = whenGetHome();

    thenHomeIsRenderedWithCuisineTypes(result, cuisineTypes);
  }

  @Test
  @DisplayName("GET / retourne 200 avec une liste vide si aucun type de cuisine actif")
  @Story("Navigation par type de cuisine")
  @Severity(SeverityLevel.NORMAL)
  @Description("Cas limite : le référentiel des types de cuisine est vide, la page "
      + "d'accueil doit tout de même s'afficher sans erreur ni section cassée.")
  void shouldReturnHomeWithEmptyListWhenNoCuisineTypes() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    final ResultActions result = whenGetHome();

    thenHomeIsRenderedWithCuisineTypes(result, List.of());
  }

  @Test
  @DisplayName("GET / reste accessible avec liste vide si tortiki-api est en panne")
  @Story("Résilience — dégradation gracieuse")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Si tortiki-api répond en erreur (500) lors de la récupération du "
      + "référentiel cuisine, la page d'accueil doit tout de même se charger avec "
      + "un statut 200 et une liste vide, plutôt que de propager une erreur 500 "
      + "au visiteur (circuit breaker soft).")
  void shouldDegradeGracefullyWhenApiIsUnavailable() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenThrow(givenFeignServerError());

    final ResultActions result = whenGetHome();

    thenHomeIsRenderedWithCuisineTypes(result, List.of());
  }

  @Test
  @DisplayName("GET / est accessible sans authentification")
  @Story("Accès public")
  @Severity(SeverityLevel.CRITICAL)
  @Description("La page d'accueil est publique : aucune redirection vers /login "
      + "ne doit se produire pour un visiteur anonyme.")
  void shouldAllowAnonymousAccessToHome() throws Exception {
    when(searchApiClient.getCuisineTypes()).thenReturn(List.of());

    mockMvc.perform(get(HOME_URL)).andExpect(status().isOk());
  }

  @Step("Préparer deux types de cuisine actifs")
  private List<CuisineTypeResponse> givenActiveCuisineTypes() {
    return List.of(
        new CuisineTypeResponse(1L, "Ukrainienne"),
        new CuisineTypeResponse(2L, "Française"));
  }

  @Step("Simuler une panne 500 de tortiki-api")
  private FeignException givenFeignServerError() {
    final Request request = Request.create(
        HttpMethod.GET, "/api/v1/cuisine-types", Collections.emptyMap(),
        null, StandardCharsets.UTF_8, new RequestTemplate());
    return new FeignException.InternalServerError(
        "Service indisponible", request, null, Collections.emptyMap());
  }

  @Step("GET / sans authentification")
  private ResultActions whenGetHome() throws Exception {
    return mockMvc.perform(get(HOME_URL));
  }

  @Step("Vérifier que la page d'accueil est rendue avec les types de cuisine attendus")
  private void thenHomeIsRenderedWithCuisineTypes(
      final ResultActions result,
      final List<CuisineTypeResponse> expectedCuisineTypes) throws Exception {
    result.andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("cuisineTypes", expectedCuisineTypes));
  }
}