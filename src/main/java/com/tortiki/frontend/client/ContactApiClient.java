package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.contact.ContactRequestSummaryResponse;
import com.tortiki.frontend.dto.contact.CreateContactRequestRequest;
import com.tortiki.frontend.dto.contact.UpdateContactRequestStatusRequest;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client Feign pour les opérations sur les demandes de contact.
 *
 * <p>Consomme les endpoints {@code /api/v1/contact-requests} et
 * {@code /api/v1/seller-dashboard/contact-requests} de tortiki-api.</p>
 */
@FeignClient(name = "contact-api", url = "${tortiki.api.url}")
public interface ContactApiClient {

  /**
   * Récupère les demandes de contact reçues par le vendeur connecté.
   *
   * @param seller email du vendeur (valeur "me" transmise par le controller)
   * @return liste des résumés de demandes
   */
  @GetMapping("/api/v1/seller-dashboard/contact-requests")
  List<ContactRequestSummaryResponse> getDashboard(@RequestParam("seller") String seller);

  /**
   * Confirme ou refuse une demande de contact.
   *
   * @param id identifiant de la demande
   * @param request DTO contenant le nouveau statut
   * @return demande mise à jour
   */
  @PatchMapping("/api/v1/seller-dashboard/contact-requests/{id}/status")
  ContactRequestSummaryResponse updateStatus(
      @PathVariable Long id,
      @RequestBody UpdateContactRequestStatusRequest request);

  /**
   * Soumet une demande de contact pour une annonce (acheteur).
   *
   * @param request DTO avec listingId, message, portions
   * @return demande créée au statut PENDING
   */
  @PostMapping("/api/v1/contact-requests")
  ContactRequestSummaryResponse submit(@RequestBody CreateContactRequestRequest request);
}