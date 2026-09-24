package com.ecommerce.apigateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tells downstream services who the caller is: sets {@value #USER_ID} to the verified token's
 * subject, and always drops any {@value #USER_ID} the client sent, so it cannot be spoofed.
 *
 * <p>Services trust this header, which is only safe because they are reachable solely through the
 * gateway (private network, no public ports).
 *
 * <p>Runs after the Spring Security filter chain (default filter order), so the authentication is
 * already resolved.
 */
@Component
public class UserIdentityFilter extends OncePerRequestFilter {

  public static final String USER_ID = "X-User-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userId = auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject() : null;
    chain.doFilter(new WithUserId(request, userId), response);
  }

  private static final class WithUserId extends HttpServletRequestWrapper {
    private final String userId;

    WithUserId(HttpServletRequest request, String userId) {
      super(request);
      this.userId = userId;
    }

    @Override
    public String getHeader(String name) {
      return USER_ID.equalsIgnoreCase(name) ? userId : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (!USER_ID.equalsIgnoreCase(name)) {
        return super.getHeaders(name);
      }
      return userId == null
          ? Collections.emptyEnumeration()
          : Collections.enumeration(List.of(userId));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      List<String> names =
          Collections.list(super.getHeaderNames()).stream()
              .filter(n -> !USER_ID.equalsIgnoreCase(n))
              .collect(Collectors.toCollection(ArrayList::new));
      if (userId != null) {
        names.add(USER_ID);
      }
      return Collections.enumeration(names);
    }
  }
}
