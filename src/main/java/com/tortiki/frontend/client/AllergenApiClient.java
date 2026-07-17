package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.listing.AllergenResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Client Feign pour la consultation des allergènes disponibles.
 *
 * <p>Consomme {@code GET /api/v1/allergens} de tortiki-api. Endpoint public,
 * utilisé côté formulaire vendeur ({@code SellerListingController}) et côté
 * fiche publique ({@code ListingController}) pour résoudre les libellés
 * lisibles à partir des identifiants bruts {@code allergenIds}.</p>
 */
@FeignClient(name = "allergen-api", url = "${tortiki.api.url}")
public interface AllergenApiClient {

  /**
   * Liste tous les allergènes disponibles sur la plateforme.
   *
   * @return liste des allergènes
   */
  @GetMapping("/api/v1/allergens")
  List<AllergenResponse> getAllergens();
}