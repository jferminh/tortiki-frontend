package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.listing.AllergenResponse;
import com.tortiki.frontend.dto.listing.CreateListingRequest;
import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import com.tortiki.frontend.dto.listing.ListingDetailResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client Feign pour la gestion des annonces vendeur.
 *
 * <p>Ce client est l'adaptateur secondaire qui isole le frontend du détail
 * HTTP de tortiki-api. Il sert au contrôleur vendeur pour créer, modifier,
 * consulter, désactiver et téléverser la photo d'une annonce.</p>
 */
@FeignClient(name = "listing-api", url = "${tortiki.api.url}")
public interface ListingApiClient {

  /**
   * Liste toutes les annonces du vendeur connecté, tous statuts confondus.
   *
   * <p>L'identité du vendeur n'est jamais transmise en paramètre : elle est
   * résolue côté {@code tortiki-api} depuis le cookie de session propagé
   * par {@code FeignConfig#sessionCookieInterceptor}. Ce choix élimine
   * toute possibilité d'IDOR (OWASP A01) qu'aurait permis un email de
   * vendeur passé librement en paramètre de requête.</p>
   *
   * @return liste des annonces du vendeur authentifié
   */
  @GetMapping("/api/v1/seller-listings")
  List<ListingDetailResponse> getMyListings();

  /**
   * Récupère le détail complet d'une annonce par son identifiant.
   *
   * @param id identifiant de l'annonce
   * @return détail de l'annonce avec photo, allergènes et avis
   */
  @GetMapping("/api/v1/listings/{id}")
  ListingDetailResponse findById(@PathVariable Long id);

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
   * Désactive une annonce (suppression logique — statut {@code INACTIVE}).
   *
   * <p>Aligné sur {@code ManageListingUseCase#delete} côté tortiki-api :
   * l'annonce n'est jamais supprimée physiquement, seul son statut change.
   * L'autorisation (vendeur propriétaire uniquement) est vérifiée côté API
   * via {@code UnauthorizedActionException}.</p>
   *
   * @param id identifiant de l'annonce à désactiver
   */
  @DeleteMapping("/api/v1/listings/{id}")
  void delete(@PathVariable Long id);

  /**
   * Téléverse ou remplace la photo associée à une annonce existante.
   *
   * <p>Aligné sur {@code ListingController#updatePhoto} côté tortiki-api,
   * qui expose cet endpoint en {@code PUT} — sémantique REST correcte
   * pour le remplacement d'une ressource déjà créée.</p>
   *
   * @param id identifiant de l'annonce
   * @param photo fichier image transmis en tant que partie multipart
   * @return annonce mise à jour avec photo
   */
  @PutMapping(value = "/api/v1/listings/{id}/photo", consumes = "multipart/form-data")
  ListingDetailResponse uploadPhoto(
      @PathVariable Long id,
      @RequestPart("photo") MultipartFile photo);

  /**
   * Liste tous les types de cuisine disponibles pour le formulaire.
   *
   * @return liste des types de cuisine
   */
  @GetMapping("/api/v1/cuisine-types")
  List<CuisineTypeResponse> getCuisineTypes();

  /**
   * Liste tous les allergènes disponibles pour le formulaire.
   *
   * @return liste des allergènes
   */
  @GetMapping("/api/v1/allergens")
  List<AllergenResponse> getAllergens();
}