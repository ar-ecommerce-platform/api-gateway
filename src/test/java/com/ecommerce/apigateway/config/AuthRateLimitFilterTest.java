package com.ecommerce.apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

  private long now = 1_000_000;
  private final AuthRateLimitFilter filter = new AuthRateLimitFilter(3, () -> now);

  @Test
  void blocksAfterLimit_perIp() throws Exception {
    for (int i = 0; i < 3; i++) {
      assertThat(status(login("1.1.1.1"))).isEqualTo(200);
    }
    MockHttpServletResponse blocked = send(login("1.1.1.1"));
    assertThat(blocked.getStatus()).isEqualTo(429);
    assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");

    assertThat(status(login("2.2.2.2"))).as("other IPs unaffected").isEqualTo(200);
  }

  @Test
  void windowResetsAfterAMinute() throws Exception {
    for (int i = 0; i < 4; i++) {
      send(login("1.1.1.1"));
    }
    now += 60_000;
    assertThat(status(login("1.1.1.1"))).isEqualTo(200);
  }

  @Test
  void onlyAuthWritesAreLimited() throws Exception {
    for (int i = 0; i < 10; i++) {
      MockHttpServletRequest browse = new MockHttpServletRequest("GET", "/api/products");
      browse.setRemoteAddr("1.1.1.1");
      assertThat(status(browse)).isEqualTo(200);
    }
  }

  private static MockHttpServletRequest login(String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr(ip);
    return request;
  }

  private int status(MockHttpServletRequest request) throws Exception {
    return send(request).getStatus();
  }

  private MockHttpServletResponse send(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }
}
