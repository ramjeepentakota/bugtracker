package com.defecttracker.config;

import com.defecttracker.entity.User;
import com.defecttracker.repository.UserClientAssignmentRepository;
import com.defecttracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class AuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserClientAssignmentRepository userClientAssignmentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip authorization for auth endpoints and public resources
        if (requestURI.startsWith("/api/auth/") ||
            requestURI.startsWith("/api/dashboard/") ||
            requestURI.startsWith("/api/test-plans/") ||
            requestURI.startsWith("/api/users/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Admin users have access to everything
                if (user.getRole() == User.Role.ADMIN) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Check specific resource access based on URL patterns
                if (isAuthorizedForResource(requestURI, user)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        // Deny access
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("Access denied");
    }

    private boolean isAuthorizedForResource(String requestURI, User user) {
        // Extract client and application IDs from URL if present
        Long clientId = extractClientId(requestURI);
        Long applicationId = extractApplicationId(requestURI);

        if (clientId != null) {
            return userClientAssignmentRepository.existsByUserIdAndClientIdOrApplicationId(
                user.getId(), clientId, applicationId != null ? applicationId : -1L);
        }

        return false;
    }

    private Long extractClientId(String requestURI) {
        // Extract client ID from URLs like /api/defects/client/123 or /api/applications/client/123
        String[] patterns = {"/api/defects/client/", "/api/applications/client/"};
        for (String pattern : patterns) {
            if (requestURI.contains(pattern)) {
                String[] parts = requestURI.split(pattern);
                if (parts.length > 1) {
                    try {
                        return Long.parseLong(parts[1].split("/")[0]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private Long extractApplicationId(String requestURI) {
        // Extract application ID from URLs like /api/defects/123 or /api/applications/123
        String[] patterns = {"/api/defects/", "/api/applications/"};
        for (String pattern : patterns) {
            if (requestURI.contains(pattern)) {
                String[] parts = requestURI.split(pattern);
                if (parts.length > 1) {
                    try {
                        return Long.parseLong(parts[1].split("/")[0]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}