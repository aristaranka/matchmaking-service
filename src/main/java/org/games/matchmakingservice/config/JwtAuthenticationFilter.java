package org.games.matchmakingservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Enhanced debug logging
        System.out.println("=== JWT Filter Processing ===");
        System.out.println("URI: " + requestURI);
        System.out.println("Method: " + method);
        
        // Check if this is a permitted path (no authentication required)
        if (isPermittedPath(requestURI)) {
            System.out.println("✅ PERMITTED PATH - Skipping JWT processing");
            System.out.println("==========================================");
            filterChain.doFilter(request, response);
            return;
        }
        
        System.out.println("🔒 PROTECTED PATH - Processing JWT authentication");
        
        // For protected paths, check if user is already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("✅ User already authenticated, proceeding");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                String subject = jwtService.getSubject(token);
                if (subject != null) {
                    UserDetails userDetails = User.withUsername(subject).password("").authorities(Collections.emptyList()).build();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("✅ JWT authentication successful for user: " + subject);
                } else {
                    System.out.println("⚠️  JWT token invalid");
                }
            } else {
                System.out.println("⚠️  No valid Authorization header found");
            }
        } catch (Exception e) {
            System.out.println("❌ JWT processing error: " + e.getMessage());
        }
        
        System.out.println("==========================================");
        filterChain.doFilter(request, response);
    }
    
    private boolean isPermittedPath(String requestURI) {
        // List of paths that don't require JWT authentication
        // This MUST match exactly with SecurityConfig.permitAll() paths
        
        // Public API endpoints
        if (requestURI.startsWith("/api/auth/")) return true;
        if (requestURI.equals("/api/match/status")) return true;
        if (requestURI.equals("/api/match/leaderboard")) return true;
        if (requestURI.equals("/api/match/history")) return true;
        
        // Static resources and pages
        if (requestURI.equals("/") || requestURI.equals("/index.html") || requestURI.equals("/home")) return true;
        if (requestURI.equals("/demo.html") || requestURI.equals("/demo.js") || requestURI.equals("/demo.css")) return true;
        if (requestURI.equals("/login.css") || requestURI.equals("/login.js") || requestURI.equals("/login") || requestURI.equals("/login.html")) return true;
        if (requestURI.equals("/auth-success") || requestURI.equals("/auth-success.html")) return true;
        if (requestURI.equals("/favicon.ico")) return true;
        
        // Directory-based paths
        if (requestURI.startsWith("/static/")) return true;
        if (requestURI.startsWith("/h2-console/")) return true;
        if (requestURI.startsWith("/actuator/")) return true;
        if (requestURI.startsWith("/ws-match/")) return true;
        
        // Allow all static resource files (HTML, JS, CSS)
        if (requestURI.endsWith(".html") || requestURI.endsWith(".js") || requestURI.endsWith(".css")) return true;
        
        // Enhanced debug logging
        System.out.println("=== JWT Filter Path Check ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Is permitted: " + false);
        System.out.println("=============================");
        
        return false;
    }
}


