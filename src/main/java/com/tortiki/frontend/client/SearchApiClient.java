package com.tortiki.frontend.client;

import com.tortiki.frontend.dto.listing.CuisineTypeResponse;
import com.tortiki.frontend.dto.listing.ListingCardResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client Feign pour les endpoints de recherche et de référentiel de {@code tortiki-api}.
 *
 * <p>Couvre deux cas d'usage :</p>
 * <ul>
 *   <li>Récupération du référentiel des types de cuisine (page d'accueil).</li>
 *   <li>Recherche géolocalisée des annonces avec filtres (page de recherche).</li>
 * </ul>
 *
 * <p>Ces endpoints sont publics côté API — aucune authentification requise.
 * Le cookie de session est néanmoins propagé par {@code FeignConfig} pour
 * les cas où l'utilisateur est connecté.</p>
 */
@FeignClient(name = "search-api", url = "${tortiki.api.url}")
public interface SearchApiClient {

  /**
   * Récupère la liste de tous les types de cuisine actifs.
   *
   * <p>Utilisé sur la page d'accueil pour afficher les cards
   * de navigation par type de cuisine.</p>
   *
   * @return liste des types de cuisine actifs
   */
  @GetMapping("/api/v1/cuisine-types")
  List<CuisineTypeResponse> getCuisineTypes();

  /**
   * Recherche des annonces selon des critères géographiques et de filtre.
   *
   * <p>Tous les paramètres sont optionnels — un appel sans paramètre
   * renvoie la première page de toutes les annonces actives.</p>
   *
   * @param query         mot-clé libre (titre, description)
   * @param city          ville de recherche
   * @param postalCode    code postal
   * @param cuisineTypeId filtre par type de cuisine
   * @param page          numéro de page (0-based)
   * @param size          nombre de résultats par page
   * @return liste des annonces correspondant aux critères
   */
  @GetMapping("/api/v1/listings/search")
  List<ListingCardResponse> search(
    @RequestParam(required = false) String query,
    @RequestParam(required = false) String city,
    @RequestParam(required = false) String postalCode,
    @RequestParam(required = false) Long cuisineTypeId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  );
}
