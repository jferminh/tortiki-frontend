package com.tortiki.frontend.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestionnaire global des exceptions du frontend Tortiki.
 *
 * <p>Complément Issue 53 : intercepte les échecs liés à l'upload multipart
 * et aux jetons CSRF invalides, pour éviter toute fuite de stacktrace
 * vers l'utilisateur final (conformité OWASP A05 — Security Misconfiguration).</p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /** Préfixe Spring MVC déclenchant une redirection HTTP. */
  private static final String REDIRECT_PREFIX = "redirect:";

  /** Clé de l'attribut flash utilisé pour transmettre un message d'erreur à la vue. */
  private static final String FLASH_ATTR_ERROR = "error";

  /** Route de connexion — cible de repli en cas de session expirée. */
  private static final String ROUTE_LOGIN = "/login";

  /** Route de repli si aucun en-tête {@code Referer} n'est disponible. */
  private static final String ROUTE_DASHBOARD_FALLBACK = "/dashboard";

  /** Chemin de la vue d'erreur 403, réutilisant les templates de l'Issue #55. */
  private static final String VIEW_ERROR_403 = "error/403";

  /** Chemin de la vue d'erreur 404, réutilisant les templates de l'Issue #55. */
  private static final String VIEW_ERROR_404 = "error/404";

  /**
   * Intercepte le dépassement de taille de fichier ou de requête multipart.
   *
   * @param ex       l'exception de dépassement de taille
   * @param request  la requête HTTP en cours, pour récupérer le referer
   * @param redirectAttributes attributs flash pour le message d'erreur
   * @return redirection vers la page précédente avec message d'erreur
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleMaxUploadSizeExceeded(
      final MaxUploadSizeExceededException ex,
      final HttpServletRequest request,
      final RedirectAttributes redirectAttributes) {
    log.warn("Taille de fichier dépassée sur {} : {}",
        request.getRequestURI(), ex.getMessage());
    redirectAttributes.addFlashAttribute(FLASH_ATTR_ERROR,
        "La photo est trop volumineuse (5 Mo maximum). Merci de réduire sa taille.");
    return REDIRECT_PREFIX + refererOrFallback(request);
  }

  /**
   * Intercepte les erreurs génériques de parsing multipart
   * (ex. flux corrompu, dépassement du nombre de parts).
   *
   * @param ex       l'exception multipart
   * @param request  la requête HTTP en cours
   * @param redirectAttributes attributs flash pour le message d'erreur
   * @return redirection vers la page précédente avec message d'erreur
   */
  @ExceptionHandler(MultipartException.class)
  public String handleMultipartException(
      final MultipartException ex,
      final HttpServletRequest request,
      final RedirectAttributes redirectAttributes) {
    log.error("Erreur de traitement du formulaire multipart sur {} : {}",
        request.getRequestURI(), ex.getMessage());
    redirectAttributes.addFlashAttribute(FLASH_ATTR_ERROR,
        "Le formulaire n'a pas pu être traité. Merci de réessayer.");
    return REDIRECT_PREFIX + refererOrFallback(request);
  }

  /**
   * Intercepte les refus d'accès Spring Security, notamment
   * les jetons CSRF invalides ou expirés (session expirée).
   *
   * @param request la requête HTTP en cours
   * @param redirectAttributes attributs flash pour le message d'erreur
   * @return redirection vers la page de connexion avec message d'erreur
   */
  @ExceptionHandler(AccessDeniedException.class)
  public String handleAccessDenied(
      final HttpServletRequest request,
      final RedirectAttributes redirectAttributes) {
    log.warn("Accès refusé (CSRF invalide ou session expirée) sur {}",
        request.getRequestURI());
    redirectAttributes.addFlashAttribute(FLASH_ATTR_ERROR,
        "Votre session a expiré. Merci de vous reconnecter et réessayer.");
    return REDIRECT_PREFIX + ROUTE_LOGIN;
  }

  /**
   * Intercepte un échec d'authentification distant (401) renvoyé par tortiki-api.
   *
   * <p>Se produit typiquement quand la session frontend est encore active,
   * mais que le jeton de session côté API a expiré ou est invalide.</p>
   *
   * @param ex      l'exception Feign 401 levée par l'appel distant
   * @param request la requête HTTP en cours
   * @return redirection vers la page de connexion avec message flash
   */
  @ExceptionHandler(FeignException.Unauthorized.class)
  public String handleFeignUnauthorized(final FeignException.Unauthorized ex,
                                        final HttpServletRequest request) {
    log.warn("Appel API non authentifié sur {} : {}", request.getRequestURI(), ex.getMessage());
    return REDIRECT_PREFIX + ROUTE_LOGIN;
  }

  /**
   * Intercepte un refus d'accès distant (403) renvoyé par tortiki-api.
   *
   * <p>Se produit quand un utilisateur authentifié tente d'accéder à une
   * ressource réservée à un autre rôle (ex. ROLE_SELLER sur une route
   * réservée à ROLE_ADMIN, malgré la garde côté frontend).</p>
   *
   * @param ex      l'exception Feign 403 levée par l'appel distant
   * @param request la requête HTTP en cours
   * @return vue d'erreur 403 avec le code HTTP correspondant
   */
  @ExceptionHandler(FeignException.Forbidden.class)
  public ModelAndView handleFeignForbidden(final FeignException.Forbidden ex,
                                           final HttpServletRequest request) {
    log.warn("Accès distant refusé sur {} : {}", request.getRequestURI(), ex.getMessage());
    final ModelAndView modelAndView = new ModelAndView(VIEW_ERROR_403);
    modelAndView.setStatus(HttpStatus.FORBIDDEN);
    return modelAndView;
  }

  /**
   * Intercepte une ressource distante introuvable (404) renvoyée par tortiki-api.
   *
   * <p>Se produit quand une annonce, un type de cuisine ou une demande de
   * contact référencée par le frontend a été supprimée ou n'existe pas
   * côté base de données.</p>
   *
   * @param ex      l'exception Feign 404 levée par l'appel distant
   * @param request la requête HTTP en cours
   * @return vue d'erreur 404 avec le code HTTP correspondant
   */
  @ExceptionHandler(FeignException.NotFound.class)
  public ModelAndView handleFeignNotFound(final FeignException.NotFound ex,
                                          final HttpServletRequest request) {
    log.warn("Ressource distante introuvable sur {} : {}",
        request.getRequestURI(), ex.getMessage());
    final ModelAndView modelAndView = new ModelAndView(VIEW_ERROR_404);
    modelAndView.setStatus(HttpStatus.NOT_FOUND);
    return modelAndView;
  }

  /**
   * Détermine la page de retour à partir du referer HTTP, avec repli
   * sur le tableau de bord vendeur si l'en-tête est absent.
   *
   * @param request la requête HTTP en cours
   * @return l'URL de retour
   */
  private String refererOrFallback(final HttpServletRequest request) {
    final String referer = request.getHeader("Referer");
    return (referer != null && !referer.isBlank()) ? referer : ROUTE_DASHBOARD_FALLBACK;
  }
}