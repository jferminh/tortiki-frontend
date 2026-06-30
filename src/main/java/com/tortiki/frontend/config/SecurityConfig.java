package com.tortiki.frontend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Configuration Spring Security du frontend Tortiki.
 *
 * <p>Stratégie retenue : sessions HTTP stateful côté frontend,
 * en cohérence avec {@code tortiki-api}. Le frontend ne gère pas
 * l'authentification lui-même — il la délègue à l'API via Feign,
 * puis stocke le {@code JSESSIONID} reçu dans la session courante.</p>
 *
 * <p>CSRF : activé sur tous les formulaires Thymeleaf. Le token CSRF
 * est injecté automatiquement via {@code th:action} de Thymeleaf +
 * {@code CookieCsrfTokenRepository}. Référence OWASP :
 * https://owasp.org/www-community/attacks/csrf</p>
 *
 * <p>Rôles : le contrôle d'accès fin (ROLE_SELLER, ROLE_BUYER, ROLE_ADMIN)
 * est délégué à {@code tortiki-api}. Le frontend protège uniquement
 * les routes sensibles pour éviter les appels inutiles à l'API.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Route de connexion. */
  private static final String ROUTE_LOGIN = "/login";

  /** Route de déconnexion. */
  private static final String ROUTE_LOGOUT = "/logout";

  /** Routes publiques — accessibles sans authentification. */
  private static final String[] PUBLIC_ROUTES = {
      "/",
      "/login",
      "/register",
      "/listings/**",
      "/search",
      "/search/**",
      "/css/**",
      "/js/**",
      "/images/**",
      "/webjars/**",
      "/actuator/health"
  };

  /** Routes réservées aux vendeurs. */
  private static final String[] SELLER_ROUTES = {
      "/dashboard/**",
      "/seller/**"
  };

  /** Routes réservées aux administrateurs. */
  private static final String[] ADMIN_ROUTES = {
      "/admin/**"
  };

  /**
   * Chaîne de filtres de sécurité principale du frontend.
   *
   * <p>Ordre de priorité des règles :</p>
   * <ol>
   *   <li>Ressources statiques et routes publiques → accès libre.</li>
   *   <li>Routes vendeur ({@code /dashboard}, {@code /seller}) → authentification requise.</li>
   *   <li>Routes admin ({@code /admin}) → authentification requise.</li>
   *   <li>Tout le reste → authentification requise.</li>
   * </ol>
   *
   * <p>Note : le contrôle ROLE_SELLER / ROLE_ADMIN est assuré côté API.
   * Si un BUYER tente d'accéder à {@code /dashboard}, l'API répondra 403,
   * et {@code GlobalExceptionHandler} redirigera vers {@code /403}.</p>
   *
   * @param http le constructeur de sécurité HTTP
   * @return la chaîne de filtres configurée
   * @throws Exception en cas d'erreur de configuration
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_ROUTES).permitAll()
            .requestMatchers(SELLER_ROUTES).authenticated()
            .requestMatchers(ADMIN_ROUTES).authenticated()
            .anyRequest().authenticated()
        )

        // CSRF activé — formulaires Thymeleaf (th:action génère le token)
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        )

        // Login : page personnalisée Thymeleaf
        .formLogin(form -> form
            .loginPage(ROUTE_LOGIN)
            .loginProcessingUrl(ROUTE_LOGIN)
            .defaultSuccessUrl("/", true)
            .failureHandler(authenticationFailureHandler())
            .permitAll()
        )

        // Logout
        .logout(logout -> logout
            .logoutUrl(ROUTE_LOGOUT)
            .logoutSuccessUrl(ROUTE_LOGIN + "?logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )

        // Redirection automatique vers /login si non authentifié
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              log.debug("Accès non authentifié sur {} → redirection /login",
                  request.getRequestURI());
              response.sendRedirect(ROUTE_LOGIN);
            })
        );

    return http.build();
  }

  /**
   * Gestionnaire d'échec d'authentification.
   *
   * <p>Redirige vers {@code /login?error} avec un message d'erreur
   * générique — sans révéler si c'est l'email ou le mot de passe
   * qui est incorrect (bonne pratique OWASP A07).</p>
   *
   * @return le gestionnaire d'échec d'authentification
   */
  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return new SimpleUrlAuthenticationFailureHandler(ROUTE_LOGIN + "?error");
  }
}