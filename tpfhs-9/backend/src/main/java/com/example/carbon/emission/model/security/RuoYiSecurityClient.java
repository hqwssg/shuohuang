package com.example.carbon.emission.model.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RuoYiSecurityClient {
    private final RestClient restClient;
    private final CarbonSecurityProperties properties;

    public RuoYiSecurityClient(RestClient.Builder builder, CarbonSecurityProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public CarbonSecurityContext load(String authorization) {
        Map<String, Object> response = restClient.get()
                .uri(properties.getEndpoint())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || number(response.get("code")) != 200 || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("RuoYi rejected the current login session");
        }
        return new CarbonSecurityContext(
                longValue(data.get("userId")),
                text(data.get("userName")),
                text(data.get("nickName")),
                longValue(data.get("deptId")),
                text(data.get("deptName")),
                strings(data.get("roles")),
                strings(data.get("permissions")),
                Boolean.TRUE.equals(data.get("allData")),
                Boolean.TRUE.equals(data.get("selfOnly")),
                longs(data.get("deptIds")),
                longs(data.get("nodeIds")));
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Set<String> strings(Object value) {
        if (!(value instanceof Iterable<?> values)) {
            return Collections.emptySet();
        }
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(String::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<Long> longs(Object value) {
        if (!(value instanceof Iterable<?> values)) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }
}
