package com.example.carbon.emission.model.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarbonPermissionResolverTest {
    @Test
    void separatesDataEntryWorkflowPermissions() {
        assertThat(CarbonPermissionResolver.requiredPermissions("POST", "/api/emission-data/12/submit"))
                .containsExactly("carbon:data:submit");
        assertThat(CarbonPermissionResolver.requiredPermissions("POST", "/api/emission-data/12/approve"))
                .containsExactly("carbon:data:review");
        assertThat(CarbonPermissionResolver.requiredPermissions("POST", "/api/emission-data/12/lock"))
                .containsExactly("carbon:data:lock");
    }

    @Test
    void separatesModelAndParameterChanges() {
        assertThat(CarbonPermissionResolver.requiredPermissions("PUT", "/api/nodes/12"))
                .containsExactly("carbon:model:edit");
        assertThat(CarbonPermissionResolver.requiredPermissions("PUT", "/api/units/12"))
                .containsExactly("carbon:params:edit");
        assertThat(CarbonPermissionResolver.requiredPermissions("PUT", "/api/meter-settings/12"))
                .contains("carbon:params:collection:edit");
    }

    @Test
    void allowsIdentityEndpointForEveryAuthenticatedUser() {
        assertThat(CarbonPermissionResolver.requiredPermissions("GET", "/api/security/me")).isEmpty();
    }
}
