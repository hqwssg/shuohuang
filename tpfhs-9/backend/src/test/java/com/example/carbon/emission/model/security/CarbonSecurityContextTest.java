package com.example.carbon.emission.model.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CarbonSecurityContextTest {
    @Test
    void matchesExactAndWildcardPermissions() {
        CarbonSecurityContext context = new CarbonSecurityContext(1L, "tester", "Tester", 2L, "Dept",
                Set.of("carbon:model:*"), Set.of("carbon:model:*"), false, false, List.of(2L), List.of());

        assertThat(context.hasPermission("carbon:model:view")).isTrue();
        assertThat(context.hasPermission("carbon:params:view")).isFalse();
    }
}
