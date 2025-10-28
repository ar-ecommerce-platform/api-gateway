package com.ecommerce.apigateway;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests for the API Gateway application. Makes sure the application starts correctly and the
 * context loads.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

  /**
   * Check that the Spring application context starts without errors. This ensures all configuration
   * and beans work correctly.
   */
  @Test
  void contextLoads() {
    // Method left intentionally empty – test passes if context loads successfully
  }

  /**
   * Check that the main method runs without throwing errors. This makes sure the application can
   * start using the main method.
   */
  @Test
  void mainRunsWithoutException() {
    String[] args = {};
    assertThatCode(() -> ApiGatewayApplication.main(args)).doesNotThrowAnyException();
  }
}
