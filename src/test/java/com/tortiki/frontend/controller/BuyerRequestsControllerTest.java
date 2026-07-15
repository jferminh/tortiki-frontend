package com.tortiki.frontend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.BuyerContactApiClient;
import com.tortiki.frontend.dto.contact.ContactRequestBuyerSummaryResponse;
import com.tortiki.frontend.dto.contact.ContactRequestStatus;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests unitaires du contrôleur {@link BuyerRequestsController}.
 *
 * <p>Utilise {@code @WebMvcTest} pour isoler la couche web sans démarrer
 * le contexte Spring complet ni appeler {@code tortiki-api}.
 * Le {@link BuyerContactApiClient} Feign est mocké.</p>
 */
@Epic("Demande de contact")
@Feature("Vue acheteur — historique des demandes")
@WebMvcTest(BuyerRequestsController.class)
@DisplayName("BuyerRequestsController — Tests unitaires")
class BuyerRequestsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BuyerContactApiClient buyerContactApiClient;

  private static final LocalDateTime FIXED_NOW =
      LocalDateTime.of(2026, Month.JULY, 15, 10, 0, 0);

  @Test
  @Story("Consultation nominale")
  @Description("L'acheteur consulte son historique non vide — vue affichée avec la liste.")
  @DisplayName("GET /buyer/requests — vue buyer-requests avec la liste des demandes")
  void shouldRenderViewWithRequestsWhenBuyerHasHistory() throws Exception {
    ContactRequestBuyerSummaryResponse request = new ContactRequestBuyerSummaryResponse(
        100L, 10L, "Bortsch ukrainien maison", null, "Sofia",
        "Je suis intéressé !", 2, ContactRequestStatus.PENDING, FIXED_NOW);

    when(buyerContactApiClient.getMyRequests()).thenReturn(List.of(request));

    mockMvc.perform(get("/buyer/requests")
            .with(user("theo@tortiki.fr").roles("BUYER"))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("buyer-requests"))
        .andExpect(model().attributeExists("requests"))
        .andExpect(model().attribute("requests", hasSize(1)));
  }

  @Test
  @Story("Consultation nominale")
  @Description("L'acheteur n'a aucune demande — vue affichée avec une liste vide.")
  @DisplayName("GET /buyer/requests — vue buyer-requests avec liste vide")
  void shouldRenderViewWithEmptyListWhenBuyerHasNoHistory() throws Exception {
    when(buyerContactApiClient.getMyRequests()).thenReturn(List.of());

    mockMvc.perform(get("/buyer/requests")
            .with(user("theo@tortiki.fr").roles("BUYER"))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("buyer-requests"))
        .andExpect(model().attribute("requests", hasSize(0)));
  }

  @Test
  @Story("Sécurité")
  @Description("Un utilisateur non authentifié est redirigé vers /login.")
  @DisplayName("GET /buyer/requests — redirection /login sans authentification")
  void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/buyer/requests")
            .with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  /**
   * Configuration de sécurité minimale pour le test — reproduit la règle
   * {@code anyRequest().authenticated()} de {@code SecurityConfig} réel
   * sans en dépendre directement (isolation {@code @WebMvcTest}).
   */
  @org.springframework.boot.test.context.TestConfiguration
  static class TestSecurityConfig {

    @org.springframework.context.annotation.Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
      http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
          .formLogin(form -> form.loginPage("/login"));
      return http.build();
    }
  }
}