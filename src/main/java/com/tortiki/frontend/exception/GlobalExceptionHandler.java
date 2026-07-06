package com.tortiki.frontend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
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