package com.aferent.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final SecretKey key;

    // Public paths — JWT validation is SKIPPED for these
    private final List<String> publicPaths = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout",
            "/payments/webhook",
            "/actuator",
            "/doctors/register/profile",
            "/doctors/register/license-upload-url",
            "/doctors/register/license-confirm",
            "/hospitals",
            "/specializations"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // allow GET /doctors/** without token — public profile viewing
        // but POST/PUT/PATCH/DELETE on /doctors/** still require token
        if (path.startsWith("/doctors") && "GET".equals(method)) {
            return chain.filter(exchange);
        }

        // check hardcoded public paths
        boolean isPublic = publicPaths.stream()
                .anyMatch(p -> path.startsWith(p) || pathMatcher.match(p + "/**", path));

        if (isPublic) {
            return chain.filter(exchange);
        }

        // Get the Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-ID", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("Authorization", "")
                    .build();

            log.info("Authenticated userId={} role={} path={}",
                    claims.getSubject(),
                    claims.get("role"),
                    path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("Invalid JWT for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
    @Override
    public int getOrder() {
        return -1;   // runs after CorrelationIdFilter
    }
}