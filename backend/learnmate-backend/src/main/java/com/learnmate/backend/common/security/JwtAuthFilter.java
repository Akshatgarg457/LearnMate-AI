package com.learnmate.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the Bearer token from the Authorization header, validates it,
 * and populates the SecurityContext. Runs once per request.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        // Skip if token is malformed / expired (don't set auth → Spring will 401)
        if (!jwtUtil.isTokenWellFormed(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Only authenticate if SecurityContext is not already populated
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String subject = jwtUtil.extractSubject(token);
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(subject);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                // Account suspended, deleted or gone: leave the request unauthenticated so it ends as a clean 401.
                chain.doFilter(request, response);
                return;
            }

            if (jwtUtil.isValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
