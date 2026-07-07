package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.UserApiClient;
import com.tortiki.frontend.dto.user.UpdateUserProfileRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur MVC pour la consultation et la mise à jour du profil
 * de l'utilisateur connecté.
 *
 * <p>Retourne des vues Thymeleaf, jamais du JSON : ce contrôleur ne
 * connaît que {@link UserApiClient}, jamais l'URL HTTP de l'API.</p>
 */
@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

  private static final String VIEW_PROFILE = "profile";
  private static final String ATTR_UPDATE_USER_PROFILE_REQUEST = "updateUserProfileRequest";
  private static final String ATTR_PROFILE = "profile";
  private static final String ATTR_SUCCESS = "success";
  private static final String REDIRECT_PROFILE = "redirect:/profile";
  private static final String BINDING_RESULT_PREFIX =
      "org.springframework.validation.BindingResult.";

  private final UserApiClient userApiClient;

  /**
   * Affiche la page de profil de l'utilisateur connecté.
   *
   * @param model     modèle Thymeleaf
   * @param principal utilisateur authentifié, injecté par Spring Security
   * @return nom de la vue {@code profile}
   */
  @GetMapping
  public String profile(final Model model, final Principal principal) {
    log.info("Chargement du profil pour {}", principal.getName());
    var currentProfile = userApiClient.getMyProfile();
    if (!model.containsAttribute(ATTR_UPDATE_USER_PROFILE_REQUEST)) {
      model.addAttribute(ATTR_UPDATE_USER_PROFILE_REQUEST,
          new UpdateUserProfileRequest(currentProfile.firstName(), currentProfile.lastName()));
    }
    model.addAttribute(ATTR_PROFILE, currentProfile);
    return VIEW_PROFILE;
  }

  /**
   * Met à jour le profil de l'utilisateur connecté.
   *
   * @param request            formulaire de mise à jour, validé par Bean Validation
   * @param bindingResult      résultat de validation, réaffiché sur erreur
   * @param redirectAttributes message flash de confirmation
   * @param principal          utilisateur authentifié
   * @return redirection vers {@code /profile} (POST/Redirect/GET)
   */
  @PostMapping("/update")
  public String updateProfile(
      @Valid @ModelAttribute(ATTR_UPDATE_USER_PROFILE_REQUEST)
      final UpdateUserProfileRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes,
      final Principal principal) {
    if (bindingResult.hasErrors()) {
      log.warn("Erreurs de validation sur le profil de {}", principal.getName());
      redirectAttributes.addFlashAttribute(
          BINDING_RESULT_PREFIX + ATTR_UPDATE_USER_PROFILE_REQUEST, bindingResult);
      redirectAttributes.addFlashAttribute(ATTR_UPDATE_USER_PROFILE_REQUEST, request);
      return REDIRECT_PROFILE;
    }
    userApiClient.updateMyProfile(request);
    log.info("Profil mis à jour pour {}", principal.getName());
    redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Profil mis à jour avec succès.");
    return REDIRECT_PROFILE;
  }
}