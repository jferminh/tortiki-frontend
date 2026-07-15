package com.tortiki.frontend.controller;

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

import com.tortiki.frontend.client.ContactApiClient;
import com.tortiki.frontend.dto.contact.ContactRequestStatus;
import com.tortiki.frontend.dto.contact.ContactRequestSummaryResponse;
import com.tortiki.frontend.dto.contact.UpdateContactRequestStatusRequest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests unitaires de la couche web {@code DashboardController}.
 *
 * <p>{@code ContactApiClient} est simulé via {@code @MockitoBean} — seule la
 * logique du contrôleur (routage, modèle, redirections) est vérifiée, jamais
 * l'appel HTTP réel vers {@code tortiki-api} (hors périmètre d'un test de
 * tranche web).</p>
 *
 * <p>Annoté pour Allure Reports : chaque test est rattaché à l'Epic
 * "Tortiki Frontend", à la Feature "Dashboard vendeur", avec une Story
 * dédiée par scénario métier, conforme à la Section 7 du dossier CDA.</p>
 */
@Slf4j
@WebMvcTest(DashboardController.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Dashboard vendeur")
class DashboardControllerTest {

  private static final String SELLER_EMAIL = "sofia@tortiki.fr";
  private static final String DASHBOARD_URL = "/dashboard";
  private static final String STATUS_URL = "/dashboard/contact-requests/{id}/status";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ContactApiClient contactApiClient;

  @Test
  @DisplayName("GET /dashboard retourne 200 et affiche les demandes du vendeur")
  @Story("Consultation du tableau de bord")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia (ROLE_SELLER) charge son dashboard et voit la liste de ses "
      + "demandes de contact reçues, rendue par ContactApiClient.getDashboard().")
  void shouldReturnDashboardWithRequestsForAuthenticatedSeller() throws Exception {
    final ContactRequestSummaryResponse request = givenPendingContactRequest();
    when(contactApiClient.getDashboard()).thenReturn(List.of(request));

    final ResultActions result = whenGetDashboardAs(SELLER_EMAIL);

    thenDashboardIsRenderedWithRequests(result, List.of(request));
  }

  @Test
  @DisplayName("GET /dashboard retourne 200 avec une liste vide si aucune demande")
  @Story("Consultation du tableau de bord")
  @Severity(SeverityLevel.NORMAL)
  @Description("Cas limite : aucune demande de contact reçue, le dashboard doit "
      + "s'afficher normalement avec une liste vide, sans erreur ni page blanche.")
  void shouldReturnDashboardWithEmptyListWhenNoRequests() throws Exception {
    when(contactApiClient.getDashboard()).thenReturn(List.of());

    final ResultActions result = whenGetDashboardAs(SELLER_EMAIL);

    thenDashboardIsRenderedWithRequests(result, List.of());
  }

  @Test
  @DisplayName("POST .../status avec CONFIRMED redirige vers /dashboard avec message flash")
  @Story("Traitement d'une demande de contact")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia confirme une demande PENDING : le contrôleur délègue à "
      + "ContactApiClient.updateStatus puis applique le pattern POST/Redirect/GET.")
  void shouldRedirectToDashboardAfterConfirmingRequest() throws Exception {
    final ResultActions result = whenUpdateStatus(1L, ContactRequestStatus.CONFIRMED);

    thenRedirectsToDashboardWithFlashMessage(result);
    thenApiClientReceivedStatusUpdate(1L, ContactRequestStatus.CONFIRMED);
  }

  @Test
  @DisplayName("POST .../status avec REFUSED redirige vers /dashboard avec message flash")
  @Story("Traitement d'une demande de contact")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Sofia refuse une demande PENDING : même contrat que la confirmation, "
      + "seul le statut transmis à l'API change.")
  void shouldRedirectToDashboardAfterRefusingRequest() throws Exception {
    final ResultActions result = whenUpdateStatus(2L, ContactRequestStatus.REFUSED);

    thenRedirectsToDashboardWithFlashMessage(result);
    thenApiClientReceivedStatusUpdate(2L, ContactRequestStatus.REFUSED);
  }

  @Test
  @DisplayName("POST .../status sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute mutation d'état sans jeton "
      + "CSRF valide doit être rejetée avant d'atteindre la logique du contrôleur.")
  void shouldRejectStatusUpdateWithoutCsrfToken() throws Exception {
    final ResultActions result = mockMvc.perform(
        post(STATUS_URL, 1L)
            .param("newStatus", "CONFIRMED")
            .with(user(SELLER_EMAIL).roles("SELLER")));

    result.andExpect(status().isForbidden());
  }

  @Step("Préparer une demande de contact au statut PENDING")
  private ContactRequestSummaryResponse givenPendingContactRequest() {
    return new ContactRequestSummaryResponse(
        1L, 10L, "Bortsch ukrainien", "Théo", "Ça m'intéresse !", 2,
        ContactRequestStatus.PENDING);
  }

  @Step("GET /dashboard authentifié en tant que {sellerEmail}")
  private ResultActions whenGetDashboardAs(final String sellerEmail) throws Exception {
    return mockMvc.perform(get(DASHBOARD_URL).with(user(sellerEmail).roles("SELLER")));
  }

  @Step("Changer le statut de la demande {id} en {newStatus}")
  private ResultActions whenUpdateStatus(
      final Long id, final ContactRequestStatus newStatus) throws Exception {
    return mockMvc.perform(post(STATUS_URL, id)
        .param("newStatus", newStatus.name())
        .with(user(SELLER_EMAIL).roles("SELLER"))
        .with(csrf()));
  }

  @Step("Vérifier que le dashboard est rendu avec les demandes attendues")
  private void thenDashboardIsRenderedWithRequests(
      final ResultActions result,
      final List<ContactRequestSummaryResponse> expectedRequests) throws Exception {
    result.andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(model().attribute("requests", expectedRequests));
  }

  @Step("Vérifier la redirection vers /dashboard avec message flash de succès")
  private void thenRedirectsToDashboardWithFlashMessage(
      final ResultActions result) throws Exception {
    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(DASHBOARD_URL))
        .andExpect(flash().attributeExists("success"));
  }

  @Step("Vérifier que ContactApiClient a reçu le bon changement de statut")
  private void thenApiClientReceivedStatusUpdate(
      final Long id, final ContactRequestStatus expectedStatus) {
    verify(contactApiClient).updateStatus(id,
        new UpdateContactRequestStatusRequest(expectedStatus));
  }
}