package com.tortiki.frontend.config;

import com.tortiki.frontend.config.security.ApiDelegatingAuthenticationProvider;
import com.tortiki.frontend.config.security.ApiLogoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Configuration Spring Security du frontend Tortiki.
 *
 * <p>Stratégie retenue : sessions HTTP stateful côté frontend,
 * en cohérence avec {@code tortiki-api}. L'authentification n'est
 * jamais vérifiée localement — elle est déléguée à
 * {@code ApiDelegatingAuthenticationProvider}, qui interroge l'API
 * et relie la session frontend à la session API via un cookie
 * stocké côté serveur.</p>
 *
 * <p>CSRF : activé sur tous les formulaires Thymeleaf. Le token CSRF
 * est injecté automatiquement via {@code th:action} de Thymeleaf +
 * {@code CookieCsrfTokenRepository}. Référence OWASP :
 * <a href="https://owasp.org/www-community/attacks/csrf">...</a></p>
 *
 * <p>Rôles : le contrôle d'accès fin (ROLE_SELLER, ROLE_BUYER, ROLE_ADMIN)
 * est délégué à {@code tortiki-api}. Le frontend protège uniquement
 * les routes sensibles pour éviter les appels inutiles à l'API.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Route racine — page d'accueil. */
  private static final String ROUTE_HOME = "/";

  /** Route de connexion. */
  private static final String ROUTE_LOGIN = "/login";

  /** Route de déconnexion. */
  private static final String ROUTE_LOGOUT = "/logout";

  /** Route d'inscription. */
  private static final String ROUTE_REGISTER = "/register";

  /** Routes publiques des annonces. */
  private static final String ROUTE_LISTINGS = "/listings/**";

  /** Route de recherche. */
  private static final String ROUTE_SEARCH = "/search";

  /** Sous-routes de recherche. */
  private static final String ROUTE_SEARCH_ALL = "/search/**";

  /** Ressources statiques CSS. */
  private static final String ROUTE_STATIC_CSS = "/css/**";

  /** Ressources statiques JS. */
  private static final String ROUTE_STATIC_JS = "/js/**";

  /** Ressources statiques images. */
  private static final String ROUTE_STATIC_IMAGES = "/images/**";

  /** Webjars (Bootstrap, etc.). */
  private static final String ROUTE_WEBJARS = "/webjars/**";

  /** Actuator health — sonde Railway/Render. */
  private static final String ROUTE_ACTUATOR_HEALTH = "/actuator/health";

  /** Page d'erreur Spring Boot — doit rester publique sous peine de boucle de redirection. */
  private static final String ROUTE_ERROR = "/error";

  /** URL de redirection après logout réussi. */
  private static final String ROUTE_LOGIN_LOGOUT = ROUTE_LOGIN + "?logout";

  /** URL de redirection après échec d'authentification. */
  private static final String ROUTE_LOGIN_ERROR = ROUTE_LOGIN + "?error";

  /** Routes publiques — accessibles sans authentification. */
  private static final String[] PUBLIC_ROUTES = {
      ROUTE_HOME,
      ROUTE_LOGIN,
      ROUTE_REGISTER,
      ROUTE_LISTINGS,
      ROUTE_SEARCH,
      ROUTE_SEARCH_ALL,
      ROUTE_STATIC_CSS,
      ROUTE_STATIC_JS,
      ROUTE_STATIC_IMAGES,
      ROUTE_WEBJARS,
      ROUTE_ACTUATOR_HEALTH,
      ROUTE_ERROR
  };

  /** Routes réservées aux vendeurs — authentification vérifiée ici, rôle vérifié côté API. */
  private static final String[] SELLER_ROUTES = {
      "/dashboard/**",
      "/seller/**"
  };

  /**
   * Routes réservées aux administrateurs — authentification vérifiée ici,
   * rôle vérifié côté API.
   */
  private static final String[] ADMIN_ROUTES = {
      "/admin/**"
  };

  /**
   * Gestionnaire d'authentification explicite, utilisant exclusivement
   * {@code ApiDelegatingAuthenticationProvider}.
   *
   * <p>Sans cette déclaration explicite, Spring Security ignorerait
   * silencieusement notre provider et retomberait sur son mécanisme
   * par défaut (utilisateur en mémoire) — faille OWASP A07 identifiée
   * à l'Issue 58.</p>
   *
   * @param provider le fournisseur d'authentification délégué à l'API
   * @return le gestionnaire d'authentification configuré
   */
  @Bean
  public AuthenticationManager authenticationManager(
      final ApiDelegatingAuthenticationProvider provider) {
    return new ProviderManager(provider);
  }

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
   * @param authenticationManager le gestionnaire d'authentification délégué à l'API
   * @return la chaîne de filtres configurée
   * @throws Exception en cas d'erreur de configuration
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      final HttpSecurity http,
      final AuthenticationManager authenticationManager, ApiLogoutHandler apiLogoutHandler)
      throws Exception {
    http
        .authenticationManager(authenticationManager)

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_ROUTES).permitAll()
            .requestMatchers(SELLER_ROUTES).authenticated()
            .requestMatchers(ADMIN_ROUTES).authenticated()
            .anyRequest().authenticated()
        )

        // CSRF activé — formulaires Thymeleaf (th:action génère et injecte le token)
        // Stockage en session HTTP (comportement par défaut Spring Security), jamais
        // exposé au navigateur via un cookie lisible en JavaScript : Tortiki est une
        // application SSR pure, aucun appel AJAX ne nécessite d'y accéder côté client.
        .csrf(csrf -> csrf
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        )

        // Login : page personnalisée Thymeleaf, authentification déléguée à l'API
        .formLogin(form -> form
            .loginPage(ROUTE_LOGIN)
            .loginProcessingUrl(ROUTE_LOGIN)
            .successHandler(new RoleBasedAuthenticationSuccessHandler())
            .failureHandler(authenticationFailureHandler())
            .permitAll()
        )

        // Logout
        .logout(logout -> logout
            .logoutUrl(ROUTE_LOGOUT)
            .addLogoutHandler(apiLogoutHandler)
            .logoutSuccessUrl(ROUTE_LOGIN_LOGOUT)
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
    return new SimpleUrlAuthenticationFailureHandler(ROUTE_LOGIN_ERROR);
  }
}