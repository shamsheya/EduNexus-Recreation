package com.edunexus.security;

import com.edunexus.common.ApiError;
import com.edunexus.user.User;
import com.edunexus.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Validates "Authorization: Bearer <jwt>". Unlike the reference filter, every token failure
 * becomes a JSON 401 and the user is re-loaded so roles are always current.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        Optional<User> user;
        try {
            String userId = jwtService.validateAndGetUserId(token);
            user = userRepository.findById(userId);
        } catch (JwtException | IllegalArgumentException ex) {
            reject(response, "Token is not valid");
            return;
        }
        if (user.isEmpty()) {
            reject(response, "Token is not valid");
            return;
        }
        User u = user.get();
        AuthenticatedUser principal = new AuthenticatedUser(u.id, u.email, Set.copyOf(u.roles));
        var authorities = u.roles.stream().map(r -> new SimpleGrantedAuthority(r.authority())).toList();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(message));
    }
}
