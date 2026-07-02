package com.tortiki.frontend.controller;

import com.tortiki.frontend.client.AuthApiClient;
import com.tortiki.frontend.dto.user.RegisterRequest;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur d'inscription. Le login et le logout sont gérés
 * nativement par Spring Security (voir {@code SecurityConfig}).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

  /** Client Feign pour l'inscription auprès de l'API. */
  private final AuthApiClient authApiClient;

  /**
   * Affiche le formulaire de connexion.
   *
   * @return nom de la vue {@code login}
   */
  @GetMapping("/login")
  public String loginForm() {
    return "login";
  }

  /**
   * Affiche le formulaire d'inscription vide.
   *
   * @param model modèle Thymeleaf
   * @return nom de la vue {@code register}
   */
  @GetMapping("/register")
  public String registerForm(final Model model) {
    model.addAttribute("registerRequest", new RegisterRequest("", "", "", "", "BUYER"));
    return "register";
  }

  /**
   * Traite la soumission du formulaire d'inscription.
   *
   * @param request données saisies, validées via Bean Validation
   * @param bindingResult résultat de la validation
   * @param redirectAttributes messages flash pour la redirection
   * @return redirection vers {@code /login} en cas de succès,
   *     sinon retour au formulaire avec les erreurs
   */
  @PostMapping("/register")
  public String register(
      @Valid @ModelAttribute("registerRequest") final RegisterRequest request,
      final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "register";
    }
    try {
      authApiClient.register(request);
      redirectAttributes.addFlashAttribute("success",
          "Compte créé avec succès. Vous pouvez vous connecter.");
      return "redirect:/login";
    } catch (FeignException.Conflict conflictException) {
      log.warn("Tentative d'inscription avec email déjà utilisé : {}", request.email());
      bindingResult.rejectValue("email", "email.exists", "Cet email est déjà utilisé.");
      return "register";
    }
  }
}