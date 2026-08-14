package com.medour.config;

import com.medour.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(GET, "/api/v1/health").permitAll()
            .requestMatchers(GET, "/api/v1/sse/slots").permitAll()
            .requestMatchers(POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers(GET, "/api/v1/appointments/my").hasRole("PATIENT")
            .requestMatchers(GET, "/api/v1/appointments/doctor/my").hasRole("DOCTOR")
            .requestMatchers(PATCH, "/api/v1/appointments/doctor/**").hasRole("DOCTOR")
            .requestMatchers(POST, "/api/v1/slots/reserve", "/api/v1/appointments", "/api/v1/ratings")
            .hasRole("PATIENT")
            .requestMatchers(PATCH, "/api/v1/ratings/**").hasRole("PATIENT")
            .anyRequest().authenticated())
        .exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, ex) -> res.sendError(SC_UNAUTHORIZED))
            .accessDeniedHandler((req, res, ex) -> {
              res.setContentType("application/json");
              res.setCharacterEncoding("UTF-8");
              res.setStatus(SC_FORBIDDEN);
              res.getWriter().write("{\"error\":\"Forbidden\"}");
              res.getWriter().flush();
            }))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:5173"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
