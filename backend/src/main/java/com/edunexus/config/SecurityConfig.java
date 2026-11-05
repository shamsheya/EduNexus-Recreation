package com.edunexus.config;

import com.edunexus.common.ApiError;
import com.edunexus.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // stateless bearer-token API
            .cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    writeError(res, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
                .accessDeniedHandler((req, res, e) ->
                    writeError(res, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Access denied")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Prevent Spring Boot from also registering the JWT filter as a plain servlet filter. */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    private static void writeError(HttpServletResponse res, ObjectMapper om, int status, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        om.writeValue(res.getOutputStream(), ApiError.of(message));
    }
}
