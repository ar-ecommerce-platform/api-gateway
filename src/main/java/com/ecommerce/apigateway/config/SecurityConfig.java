package com.ecommerce.apigateway.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Enforces authentication at the single public entry point. Open to anyone: {@code /api/auth/**}
 * (login, register), actuator health/info (for load-balancer checks), and read-only browsing of the
 * catalog ({@code GET} on {@code /api/products/**} and {@code /api/inventory/**}) so the storefront
 * is browsable without an account. With a valid bearer token: orders, reading your notifications,
 * and {@code /api/users/me}. The API docs ({@code /swagger-ui.html}) are public. Everything else is
 * denied - it is internal to the platform.
 *
 * <p>{@link UserIdentityFilter} then tells the services who the caller is.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

  private final SecurityProperties properties;

  public SecurityConfig(SecurityProperties properties) {
    this.properties = properties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    // API docs: the Swagger page and each service's spec. Calls made from the
                    // page still go through the rules below.
                    .requestMatchers(
                        HttpMethod.GET,
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/*/api-docs")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/inventory/**")
                    .permitAll()
                    .requestMatchers("/api/orders/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/notifications", "/api/users/me")
                    .authenticated()
                    // Default deny: payments, stock reservation, notification writes and the
                    // user directory are service-to-service only, never reachable by clients.
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver()));
    return http.build();
  }

  @Bean
  public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
    return new JwtIssuerAuthenticationManagerResolver(
        JwtAuthenticationManagers.byIssuer(properties)::get);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(properties.getCors().getAllowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
