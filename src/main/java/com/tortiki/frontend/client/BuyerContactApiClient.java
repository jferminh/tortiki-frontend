package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.contact.ContactRequestBuyerSummaryResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Client Feign pour l'historique des demandes de contact de l'acheteur.
 *
 * <p>Délègue les appels REST à l'endpoint {@code GET /api/v1/contact-requests/my}
 * de {@code tortiki-api}. La session {@code JSESSIONID} est propagée
 * automatiquement par {@code FeignConfig}.</p>
 */
@FeignClient(name = "buyer-contact-api", url = "${tortiki.api.url}")
public interface BuyerContactApiClient {

  /**
   * Récupère l'historique des demandes de contact de l'acheteur connecté.
   *
   * @return liste des demandes de contact, vide si aucune
   */
  @GetMapping("/api/v1/contact-requests/my")
  List<ContactRequestBuyerSummaryResponse> getMyRequests();
}