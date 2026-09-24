package com.ecommerce.apigateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Slows password guessing: at most {@code limit} login/register attempts per client IP per minute,
 * then {@code 429 Too Many Requests}. Runs before Spring Security, so rejected attempts are cheap.
 *
 * <p>The client IP is {@link HttpServletRequest#getRemoteAddr()}, which honours {@code
 * X-Forwarded-For} from trusted proxies only ({@code server.forward-headers-strategy: native}).
 */
// ponytail: in-memory, per gateway instance - N instances allow N x limit. Move to an edge rule
// (AWS WAF rate-based rule) or a shared store (Redis + Bucket4j) when running more than one.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_MS = 60_000;
  private static final int MAX_TRACKED_IPS = 10_000;

  private final int limit;
  private final LongSupplier nowMs;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(long startMs, AtomicInteger count) {}

  @Autowired
  public AuthRateLimitFilter(@Value("${security.auth-rate-limit.per-minute:10}") int limit) {
    this(limit, System::currentTimeMillis);
  }

  AuthRateLimitFilter(int limit, LongSupplier nowMs) {
    this.limit = limit;
    this.nowMs = nowMs;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !("POST".equals(request.getMethod())
        && request.getRequestURI().startsWith("/api/auth/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = nowMs.getAsLong();
    if (windows.size() > MAX_TRACKED_IPS) {
      windows.values().removeIf(w -> now - w.startMs() >= WINDOW_MS);
    }
    Window window =
        windows.compute(
            request.getRemoteAddr(),
            (ip, current) ->
                current == null || now - current.startMs() >= WINDOW_MS
                    ? new Window(now, new AtomicInteger())
                    : current);

    if (window.count().incrementAndGet() > limit) {
      long retryAfterSec =
          Math.max(1, Duration.ofMillis(window.startMs() + WINDOW_MS - now).toSeconds());
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader("Retry-After", String.valueOf(retryAfterSec));
      return;
    }
    chain.doFilter(request, response);
  }
}
