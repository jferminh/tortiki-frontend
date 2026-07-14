package com.tortiki.frontend.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure le client HTTP sous-jacent utilisé par Feign.
 *
 * <p>Apache HttpClient gère nativement les cookies via son propre
 * {@code CookieStore} interne. Cette gestion automatique intercepte
 * le header {@code Set-Cookie} de la réponse avant que Feign ne
 * construise la {@code ResponseEntity} exposée à l'application ce
 * qui rend le header invisible pour {@code ApiDelegatingAuthenticationProvider}.</p>
 *
 * <p>Ici, la session API est gérée manuellement, côté serveur, dans
 * la session HTTP du frontend voir Issue 58. Il faut donc désactiver
 * la gestion automatique des cookies d'Apache HttpClient pour que le
 * {@code Set-Cookie} brut reste lisible dans les headers de réponse Feign.</p>
 */
@Configuration
public class FeignHttpClientConfig {

  /**
   * Fournit un client Feign basé sur Apache HttpClient sans gestion
   * automatique des cookies.
   *
   * @return le client Feign configuré
   */
  @Bean
  public Client feignClient() {
    return new ApacheHttpClient(HttpClientBuilder.create()
        .disableCookieManagement()
        .setConnectionTimeToLive(30, TimeUnit.SECONDS)
        .build());
  }
}