package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.admin.AdminListingSummaryResponse;
import com.tortiki.frontend.dto.admin.UpdateListingStatusRequest;
import com.tortiki.frontend.dto.listing.CreateCuisineTypeRequest;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour les opérations d'administration.
 *
 * <p>Couvre deux périmètres réservés au rôle {@code ROLE_ADMIN} dans tortiki-api :</p>
 * <ul>
 *   <li>Modération des annonces (consultation de toutes les annonces,
 *   activation/désactivation).</li>
 *   <li>Gestion des types de cuisine (CRUD complet).</li>
 * </ul>
 *
 * <p>Ce client est distinct de {@code ListingApiClient} et {@code SearchApiClient} car
 * ses endpoints exigent systématiquement {@code ROLE_ADMIN} : les appels depuis un autre
 * rôle retourneraient 403 sans exception.</p>
 */
@FeignClient(name = "admin-api", url = "${tortiki.api.url}")
public interface AdminApiClient {

  // === LISTINGS ===

  /**
   * Récupère toutes les annonces de la plateforme (tous vendeurs, tous statuts).
   *
   * @return liste des résumés d'annonces
   */
  @GetMapping("/api/v1/admin/listings")
  List<AdminListingSummaryResponse> getAllListings();

  /**
   * Modifie le statut d'une annonce (activation, désactivation, suppression logique).
   *
   * @param id      identifiant de l'annonce à modifier
   * @param request DTO contenant le nouveau statut
   * @return résumé de l'annonce avec son statut mis à jour
   */
  @PatchMapping("/api/v1/admin/listings/{id}/status")
  AdminListingSummaryResponse updateListingStatus(
      @PathVariable Long id,
      @RequestBody UpdateListingStatusRequest request);

  // === CUISINE TYPES ===

  /**
   * Crée un nouveau type de cuisine.
   *
   * @param request DTO contenant le nom du type de cuisine
   * @return type de cuisine créé avec son identifiant
   */
  @PostMapping("/api/v1/admin/cuisine-types")
  CuisineTypeResponse createCuisineType(@RequestBody CreateCuisineTypeRequest request);

  /**
   * Supprime un type de cuisine par son identifiant.
   *
   * @param id identifiant du type de cuisine à supprimer
   */
  @DeleteMapping("/api/v1/admin/cuisine-types/{id}")
  void deleteCuisineType(@PathVariable Long id);
}