package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tortiki.frontend.client.ContactApiClient;
import com.tortiki.frontend.config.SecurityConfig;
import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.dto.contact.ContactRequestStatus;
import com.tortiki.frontend.dto.contact.ContactRequestSummaryResponse;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests unitaires de la couche web {@code ContactController}.
 *
 * <p>{@code ContactApiClient} est simulé via {@code @MockitoBean}.
 * {@code SecurityConfig} est importé pour vérifier que {@code
 * /contact-requests} exige bien une authentification, cette route
 * n'étant pas déclarée dans {@code PUBLIC_ROUTES}.</p>
 */
@WebMvcTest(ContactController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Epic("Tortiki Frontend")
@Feature("Demandes de contact")
class ContactControllerTest {

  private static final String CONTACT_REQUESTS_URL = "/contact-requests";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ContactApiClient contactApiClient;

  @MockitoBean
  private ApiDelegatingAuthenticationProvider authenticationProvider;

  @Test
  @DisplayName("POST /contact-requests sans authentification redirige vers /login")
  @Story("Accès restreint")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Vérifie le comportement réel de SecurityConfig : /contact-requests "
      + "n'est pas dans PUBLIC_ROUTES, un visiteur anonyme doit être redirigé.")
  void shouldRedirectToLoginWhenAnonymousSubmitsContactRequest() throws Exception {
    mockMvc.perform(post(CONTACT_REQUESTS_URL)
            .with(csrf())
            .param("listingId", "1")
            .param("message", "Bonjour, je souhaite réserver ce plat.")
            .param("portions", "2"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("POST /contact-requests avec des données valides redirige vers la fiche plat")
  @Story("Soumission d'une demande de contact")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Théo (acheteur authentifié) soumet une demande de contact valide "
      + "pour l'annonce 1 : le contrôleur délègue à ContactApiClient.submit puis "
      + "redirige vers la fiche plat avec un message flash de confirmation.")
  void shouldRedirectToListingAfterSuccessfulSubmission() throws Exception {
    when(contactApiClient.submit(new CreateContactRequestRequest(
        1L, "Bonjour, je souhaite réserver ce plat.", 2)))
        .thenReturn(givenPendingContactRequest());

    final ResultActions result = whenSubmitContactRequest(
        "Bonjour, je souhaite réserver ce plat.", "2");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/listings/1"))
        .andExpect(flash().attributeExists("success"));
    verify(contactApiClient).submit(new CreateContactRequestRequest(
        1L, "Bonjour, je souhaite réserver ce plat.", 2));
  }

  @Test
  @DisplayName("POST /contact-requests avec un message vide réaffiche la fiche plat sans appel API")
  @Story("Soumission — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un message vide doit être rejeté par Bean Validation (@NotBlank) "
      + "avant tout appel à ContactApiClient, avec redirection vers la fiche plat "
      + "et les erreurs exposées en attribut flash BindingResult.")
  void shouldRedirectWithValidationErrorsWhenMessageIsBlank() throws Exception {
    final ResultActions result = whenSubmitContactRequest("", "2");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/listings/1"))
        .andExpect(flash().attributeExists(
            "org.springframework.validation.BindingResult.contactRequest"));
    verify(contactApiClient, never()).submit(any());
  }

  @Test
  @DisplayName("POST /contact-requests avec 0 portion réaffiche la fiche plat sans appel API")
  @Story("Soumission — validation")
  @Severity(SeverityLevel.NORMAL)
  @Description("Un nombre de portions à 0 doit être rejeté par Bean Validation "
      + "(@Min(1)), évitant une demande incohérente transmise à tortiki-api.")
  void shouldRedirectWithValidationErrorsWhenPortionsIsZero() throws Exception {
    final ResultActions result = whenSubmitContactRequest(
        "Bonjour, je souhaite réserver ce plat.", "0");

    result.andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/listings/1"))
        .andExpect(flash().attributeExists(
            "org.springframework.validation.BindingResult.contactRequest"));
    verify(contactApiClient, never()).submit(any());
  }

  @Test
  @DisplayName("POST /contact-requests sans jeton CSRF est rejeté en 403")
  @Story("Sécurité — protection CSRF")
  @Severity(SeverityLevel.BLOCKER)
  @Description("Défense en profondeur OWASP A01 : toute soumission de demande de "
      + "contact sans jeton CSRF valide doit être rejetée, y compris pour un "
      + "acheteur authentifié.")
  void shouldRejectSubmissionWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(CONTACT_REQUESTS_URL)
            .with(user("theo@tortiki.fr").roles("BUYER"))
            .param("listingId", "1")
            .param("message", "Bonjour, je souhaite réserver ce plat.")
            .param("portions", "2"))
        .andExpect(status().isForbidden());
  }

  @Step("Préparer une demande de contact au statut PENDING")
  private ContactRequestSummaryResponse givenPendingContactRequest() {
    return new ContactRequestSummaryResponse(
        1L, 1L, "Bortsch ukrainien", "Théo",
        "Bonjour, je souhaite réserver ce plat.", 2, ContactRequestStatus.PENDING);
  }

  @Step("Soumettre une demande de contact authentifiée pour l'annonce 1")
  private ResultActions whenSubmitContactRequest(
      final String message, final String portions) throws Exception {
    return mockMvc.perform(post(CONTACT_REQUESTS_URL)
        .with(user("theo@tortiki.fr").roles("BUYER"))
        .with(csrf())
        .param("listingId", "1")
        .param("message", message)
        .param("portions", portions));
  }
}