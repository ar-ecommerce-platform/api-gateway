package com.ecommerce.apigateway.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** What an anonymous caller can and cannot reach through the gateway. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired private MockMvc mvc;

  @Test
  void health_isOpen() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void otherActuatorEndpoints_requireAuth() throws Exception {
    mvc.perform(get("/actuator/gateway/routes")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  void writes_requireAuth() throws Exception {
    mvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/products")).andExpect(status().isUnauthorized());
  }

  @Test
  void catalogBrowsing_isOpen() throws Exception {
    // Passing security means the request reaches routing, which fails only because no
    // product-service is running in the test.
    assertThatThrownBy(() -> mvc.perform(get("/api/products")))
        .hasMessageContaining("Unable to find instance for product-service");
  }
}
