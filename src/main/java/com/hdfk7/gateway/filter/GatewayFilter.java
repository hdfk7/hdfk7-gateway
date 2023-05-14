package com.hdfk7.gateway.filter;

import com.hdfk7.gateway.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayFilter implements GlobalFilter, Ordered {
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private final ApplicationConfig applicationConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI requestUri = request.getURI();
        String schema = requestUri.getScheme();
        if (((!"http".equals(schema) && !"https".equals(schema))) || checkExcludeUri(request.getURI().getPath())) {
            return chain.filter(exchange);
        }
        exchange = filling(exchange);
        return returnMono(chain, exchange);
    }

    private boolean checkExcludeUri(String uri) {
        String excludeUrl = applicationConfig.getExcludeUrl();
        if (StringUtils.isBlank(excludeUrl)) {
            return Boolean.FALSE;
        }
        return Arrays.stream(excludeUrl.split(",")).anyMatch(item -> ANT_PATH_MATCHER.match(item, uri));
    }

    private ServerWebExchange filling(ServerWebExchange exchange) {
        ServerHttpRequest host = exchange.getRequest().mutate()
                .build();
        return exchange.mutate().request(host).build();
    }

    private Mono<Void> returnMono(GatewayFilterChain chain, ServerWebExchange exchange) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
