package com.aferent.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final SecretKey key;
    private final List<String> publicPaths;

        // Fallback public paths (used if app.public-paths is missing)
        private static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout",
            "/payments/webhook",
            "/actuator/**",
            "/doctors/register/profile",
            "/doctors/register/license-upload-url",
            "/doctors/register/license-confirm",
            "/hospitals/**",
            "/specializations/**",
            "/prescriptions/**",
            "/sessions/webhooks",
            "/debug/**"
        );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(
            @Value("${app.jwt.secret}") String secret,
            Environment environment
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        List<String> configuredPublicPaths = Binder.get(environment)
                .bind("app.public-paths", Bindable.listOf(String.class))
                .orElse(List.of());

        List<String> normalized = new ArrayList<>();
        if (configuredPublicPaths != null) {
            for (String path : configuredPublicPaths) {
                if (path != null && !path.isBlank()) {
                    normalized.add(path.trim());
                }
            }
        }
        this.publicPaths = normalized.isEmpty() ? DEFAULT_PUBLIC_PATHS : List.copyOf(normalized);

        log.info("Loaded {} public path pattern(s) for JWT bypass: {}", this.publicPaths.size(), this.publicPaths);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // allow only GET /doctors/** without token — public reads
        // but POST/PUT/PATCH/DELETE on /doctors/** still require token
        boolean isPublicGet = "GET".equals(method) && pathMatcher.match("/doctors/**", path);

        if (isPublicGet) {
            return chain.filter(exchange);
        }

        // check configured public paths from application.yaml (supports exact and Ant patterns)
        boolean isPublic = publicPaths.stream()
            .anyMatch(p -> matchesPublicPath(path, p));

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

                // For auth-service paths, preserve Authorization so auth-service can
                // perform role-sensitive operations like /auth/admin/** and /auth/deactivate.
                boolean isAuthServicePath = path.startsWith("/auth/");

                // For non-auth services, strip raw JWT and forward identity headers only.
                ServerHttpRequest.Builder requestBuilder = exchange.getRequest()
                    .mutate()
                    .header("X-User-ID", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-User-Email", claims.get("email", String.class));

                if (isAuthServicePath) {
                    requestBuilder.header("Authorization", authHeader);
                } else {
                    requestBuilder.headers(headers -> headers.remove("Authorization")); // strip JWT outside auth-service
                }

                ServerHttpRequest mutatedRequest = requestBuilder.build();

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

    private boolean matchesPublicPath(String requestPath, String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return false;
        }

        String pattern = configuredPath.trim();

        // If path contains wildcard tokens, treat it as an Ant pattern directly.
        if (pattern.contains("*") || pattern.contains("?")) {
            return pathMatcher.match(pattern, requestPath);
        }

        // Exact match or prefix segment match ("/foo" should match "/foo/bar", not "/foobar").
        return requestPath.equals(pattern) || requestPath.startsWith(pattern + "/");
    }
}