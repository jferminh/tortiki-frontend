package com.tortiki.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tortiki.frontend.client.ListingApiClient;
import com.tortiki.frontend.dto.listing.ListingDetailResponse;
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
 * <p>Vérifie que le formulaire de création/édition d'annonce transmet
 * bien à l'API un payload conforme au contrat réel de {@code tortiki-api}
 * (champs {@code portions}, {@code pickupAddress}, {@code pickupDatetime}).</p>
 */
@WebMvcTest(SellerListingController.class)
@ActiveProfiles("test")
@DisplayName("SellerListingController")
class SellerListingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListingApiClient listingApiClient;

  private ListingDetailResponse detail;

  @BeforeEach
  void setUp() {
    detail = new ListingDetailResponse(
        1L, "Bortsch ukrainien", "Soupe traditionnelle",
        new BigDecimal("8.50"), 4, 1L, List.of(), "12 rue de la Paix, 54000 Nancy",
        LocalDateTime.now().plusDays(7), null);
  }

  @Test
  @DisplayName("GET /seller/listings/new affiche le formulaire vide")
  void newListingForm_shouldReturnFormView() throws Exception {
    mockMvc.perform(get("/seller/listings/new")
            .with(user("sofia@tortiki.fr").roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"));
  }

  @Test
  @DisplayName("POST /seller/listings/new avec champs manquants réaffiche le formulaire")
  void createListing_shouldReturnFormView_whenRequiredFieldsMissing() throws Exception {
    mockMvc.perform(post("/seller/listings/new")
            .with(csrf())
            .with(user("sofia@tortiki.fr").roles("SELLER"))
            .param("title", "Bortsch")
            .param("price", "8.50"))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "pickupDatetime"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "portions"))
        .andExpect(model().attributeHasFieldErrors("listingRequest", "pickupAddress"));
  }

  @Test
  @DisplayName("POST /seller/listings/new valide crée l'annonce et redirige")
  void createListing_shouldRedirect_whenPayloadValid() throws Exception {
    when(listingApiClient.create(any())).thenReturn(detail);

    mockMvc.perform(post("/seller/listings/new")
            .with(csrf())
            .with(user("sofia@tortiki.fr").roles("SELLER"))
            .param("title", "Bortsch ukrainien")
            .param("description", "Soupe traditionnelle")
            .param("price", "8.50")
            .param("portions", "4")
            .param("pickupAddress", "12 rue de la Paix, 54000 Nancy")
            .param("pickupDatetime", LocalDateTime.now().plusDays(7).toString())
            .param("cuisineTypeId", "1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/seller/listings"));

    verify(listingApiClient).create(any());
  }

  @Test
  @DisplayName("GET /seller/listings/{id}/edit pré-remplit le formulaire")
  void editListingForm_shouldPrefillFields() throws Exception {
    when(listingApiClient.getById(anyLong())).thenReturn(detail);

    mockMvc.perform(get("/seller/listings/1/edit")
            .with(user("sofia@tortiki.fr").roles("SELLER")))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-form"))
        .andExpect(model().attributeExists("listingRequest"));
  }
}