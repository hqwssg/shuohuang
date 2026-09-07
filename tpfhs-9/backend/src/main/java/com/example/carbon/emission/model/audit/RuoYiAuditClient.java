package com.example.carbon.emission.model.audit;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RuoYiAuditClient {
    private static final Logger log = LoggerFactory.getLogger(RuoYiAuditClient.class);
    private static final String SECRET_HEADER = "X-Goview-Audit-Secret";

    private final RuoYiAuditProperties properties;
    private final RestClient restClient;

    public RuoYiAuditClient(RuoYiAuditProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void send(Map<String, Object> audit) {
        sendTo(endpoint("operation"), audit);
    }

    public void sendJob(Map<String, Object> jobLog) {
        sendTo(endpoint("job"), jobLog);
    }

    private String endpoint(String kind) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null) return null;
        return endpoint.endsWith("/operation")
                ? endpoint.substring(0, endpoint.length() - "/operation".length()) + "/" + kind
                : endpoint + "/" + kind;
    }

    private void sendTo(String endpoint, Map<String, Object> payload) {
        if (!properties.isEnabled() || isBlank(properties.getEndpoint()) || isBlank(properties.getSecret())) {
            return;
        }
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                restClient.post()
                        .uri(endpoint)
                        .header(SECRET_HEADER, properties.getSecret())
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (Exception failure) {
                lastFailure = failure;
                if (attempt == 1) {
                    try {
                        Thread.sleep(150L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.warn("RuoYi audit delivery failed after retry: endpoint={}, error={}", endpoint,
                lastFailure == null ? "unknown" : lastFailure.getMessage());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
