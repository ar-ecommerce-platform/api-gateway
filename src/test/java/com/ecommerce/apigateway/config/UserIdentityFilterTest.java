package com.ecommerce.apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserIdentityFilterTest {

  private final UserIdentityFilter filter = new UserIdentityFilter();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void signedIn_setsUserIdFromToken_andIgnoresSpoofedHeader() throws Exception {
    Jwt token = Jwt.withTokenValue("t").header("alg", "none").subject("ada@example.com").build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));

    HttpServletRequest seen = run(spoofed());

    assertThat(seen.getHeader("X-User-Id")).isEqualTo("ada@example.com");
    assertThat(Collections.list(seen.getHeaders("x-user-id"))).containsExactly("ada@example.com");
    assertThat(Collections.list(seen.getHeaderNames()))
        .filteredOn(n -> n.equalsIgnoreCase("X-User-Id"))
        .hasSize(1);
  }

  @Test
  void anonymous_stripsSpoofedHeader() throws Exception {
    HttpServletRequest seen = run(spoofed());

    assertThat(seen.getHeader("X-User-Id")).isNull();
    assertThat(Collections.list(seen.getHeaders("X-User-Id"))).isEmpty();
    assertThat(Collections.list(seen.getHeaderNames()))
        .noneMatch(n -> n.equalsIgnoreCase("X-User-Id"));
  }

  private static MockHttpServletRequest spoofed() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.addHeader("x-user-id", "victim@example.com");
    return request;
  }

  private HttpServletRequest run(MockHttpServletRequest request) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return (HttpServletRequest) chain.getRequest();
  }
}
