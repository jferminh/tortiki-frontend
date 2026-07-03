package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.listing.CreateListingRequest;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import com.tortiki.frontend.dto.listing.ListingDetailResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client Feign pour la gestion des annonces vendeur.
 *
 * <p>Ce client est l'adaptateur secondaire qui isole le frontend du détail
 * HTTP de tortiki-api. Il sert au contrôleur vendeur pour créer, modifier,
 * consulter et téléverser la photo d'une annonce.</p>
 */
@FeignClient(name = "listing-api", url = "${tortiki.api.url}")
public interface ListingApiClient {

  /**
   * Liste les annonces du vendeur connecté.
   *
   * @param sellerEmail email du vendeur
   * @return liste des annonces
   */
  @GetMapping("/api/v1/seller-listings")
  List<ListingDetailResponse> getMyListings(@RequestParam("seller") String sellerEmail);

  /**
   * Récupère le détail d'une annonce par son identifiant.
   *
   * @param id identifiant de l'annonce
   * @return détail complet de l'annonce
   */
  @GetMapping("/api/v1/listings/{id}")
  ListingDetailResponse getById(@PathVariable Long id);

  /**
   * Crée une nouvelle annonce.
   *
   * @param request données du formulaire
   * @return annonce créée
   */
  @PostMapping("/api/v1/listings")
  ListingDetailResponse create(@RequestBody CreateListingRequest request);

  /**
   * Met à jour une annonce existante.
   *
   * @param id identifiant de l'annonce
   * @param request données mises à jour
   * @return annonce mise à jour
   */
  @PutMapping("/api/v1/listings/{id}")
  ListingDetailResponse update(@PathVariable Long id, @RequestBody CreateListingRequest request);

  /**
   * Téléverse la photo associée à une annonce.
   *
   * @param id identifiant de l'annonce
   * @param photo fichier image
   * @return annonce mise à jour avec photo
   */
  @PostMapping(value = "/api/v1/listings/{id}/photo", consumes = "multipart/form-data")
  ListingDetailResponse uploadPhoto(@PathVariable Long id, @RequestParam MultipartFile photo);

  /**
   * Liste tous les types de cuisine disponibles pour le formulaire.
   *
   * @return liste des types de cuisine
   */
  @GetMapping("/api/v1/cuisine-types")
  List<CuisineTypeResponse> getCuisineTypes();
}