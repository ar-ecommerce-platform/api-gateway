package com.ecommerce.apigateway.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The public API surface: what anonymous and signed-in callers can reach through the gateway. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired private MockMvc mvc;

  @Test
  void health_isOpen() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void swaggerUi_isOpen_andListsTheServices() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    mvc.perform(get("/v3/api-docs/swagger-config"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/api/orders/api-docs")));
  }

  @Test
  void serviceSpecs_areOpen_evenWhereTheApiIsNot() {
    // Reaches routing (no order-service in the test) instead of being rejected with 401.
    assertThatThrownBy(() -> mvc.perform(get("/api/orders/api-docs")))
        .hasMessageContaining("Unable to find instance for order-service");
  }

  @Test
  void otherActuatorEndpoints_areClosed() throws Exception {
    mvc.perform(get("/actuator/gateway/routes")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  void orders_requireAuth() throws Exception {
    mvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());
  }

  @Test
  void catalogBrowsing_isOpen() throws Exception {
    // Passing security means the request reaches routing, which fails only because no
    // product-service is running in the test.
    assertThatThrownBy(() -> mvc.perform(get("/api/products")))
        .hasMessageContaining("Unable to find instance for product-service");
  }

  @Test
  void internalEndpoints_areDenied_evenWithAToken() throws Exception {
    mvc.perform(get("/api/payments/1").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(post("/api/payments").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(post("/api/inventory/1/reserve").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(post("/api/notifications").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(get("/api/users").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(get("/api/users/1").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(post("/api/products").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void signedInRoutes_passSecurity() {
    assertThatThrownBy(() -> mvc.perform(get("/api/users/me").with(jwt())))
        .hasMessageContaining("Unable to find instance for user-service");
    assertThatThrownBy(() -> mvc.perform(get("/api/notifications").with(jwt())))
        .hasMessageContaining("Unable to find instance for notification-service");
  }
}
