package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.ContactApiClient;
import com.tortiki.frontend.dto.contact.ContactRequestStatus;
import com.tortiki.frontend.dto.contact.ContactRequestSummaryResponse;
import com.tortiki.frontend.dto.contact.UpdateContactRequestStatusRequest;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur du tableau de bord vendeur.
 *
 * <p>Réservé aux utilisateurs authentifiés avec le rôle {@code ROLE_SELLER},
 * vérifié à la fois côté {@code SecurityConfig} du frontend et par
 * {@code tortiki-api} en profondeur (défense en profondeur).</p>
 */
@Slf4j
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private static final String VIEW_DASHBOARD = "dashboard";
  private static final String ATTR_REQUESTS = "requests";
  private static final String ATTR_SUCCESS = "success";
  private static final String REDIRECT_DASHBOARD = "redirect:/dashboard";

  private final ContactApiClient contactApiClient;

  /**
   * Affiche le tableau de bord vendeur avec les demandes de contact reçues.
   *
   * @param model modèle Thymeleaf
   * @param principal utilisateur authentifié (email résolu par Spring Security)
   * @return nom de la vue {@code dashboard}
   */
  @GetMapping
  public String dashboard(final Model model, final Principal principal) {
    log.info("Chargement du dashboard vendeur pour : {}", principal.getName());
    final List<ContactRequestSummaryResponse> requests =
        contactApiClient.getDashboard(principal.getName());
    model.addAttribute(ATTR_REQUESTS, requests);
    return VIEW_DASHBOARD;
  }

  /**
   * Confirme ou refuse une demande de contact depuis le dashboard.
   *
   * @param id identifiant de la demande
   * @param newStatus nouveau statut souhaité (CONFIRMED ou REFUSED)
   * @param redirectAttributes message flash de confirmation
   * @return redirection vers {@code /dashboard}
   */
  @PostMapping("/contact-requests/{id}/status")
  public String updateStatus(
      @PathVariable final Long id,
      @RequestParam final ContactRequestStatus newStatus,
      final RedirectAttributes redirectAttributes) {
    contactApiClient.updateStatus(id, new UpdateContactRequestStatusRequest(newStatus));
    log.info("Statut de la demande {} mis à jour : {}", id, newStatus);
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS,
        "Demande " + newStatus.name().toLowerCase() + "e avec succès.");
    return REDIRECT_DASHBOARD;
  }
}