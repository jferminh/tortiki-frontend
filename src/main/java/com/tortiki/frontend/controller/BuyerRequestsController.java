package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.BuyerContactApiClient;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrôleur MVC pour l'historique des demandes de contact de l'acheteur.
 *
 * <p>Affiche la vue {@code buyer-requests.html} listant toutes les demandes
 * soumises par Théo, tous statuts confondus. Symétrique côté acheteur de
 * {@code DashboardController} (vue vendeur).</p>
 *
 * <p>Contrairement aux {@code @RestController} de {@code tortiki-api}, ce
 * contrôleur retourne un nom de vue Thymeleaf, jamais du JSON.</p>
 */
@Slf4j
@Controller
@RequestMapping("/buyer/requests")
@RequiredArgsConstructor
public class BuyerRequestsController {

  private final BuyerContactApiClient buyerContactApiClient;

  /**
   * Affiche l'historique des demandes de contact de l'acheteur connecté.
   *
   * <p>La liste est récupérée via {@link BuyerContactApiClient}, qui
   * transmet automatiquement le cookie de session à {@code tortiki-api}
   * pour résoudre l'identité — aucun paramètre acheteur n'est manipulé ici.</p>
   *
   * @param model    modèle Thymeleaf enrichi avec la liste des demandes
   * @param principal utilisateur authentifié, utilisé uniquement pour le log
   * @return le nom de la vue {@code buyer-requests}
   */
  @GetMapping
  public String showMyRequests(Model model, Principal principal) {
    log.debug("Consultation historique demandes de contact par {}", principal.getName());

    model.addAttribute("requests", buyerContactApiClient.getMyRequests());

    return "buyer-requests";
  }
}