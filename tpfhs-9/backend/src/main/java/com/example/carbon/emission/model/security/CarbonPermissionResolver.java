package com.example.carbon.emission.model.security;

import java.util.List;

public final class CarbonPermissionResolver {
    private static final List<String> MODEL_PATHS = List.of("/api/template", "/api/nodes");
    private static final List<String> COLLECTION_PATHS = List.of(
            "/api/meter-settings", "/api/fossil-fuel-collection", "/api/purchased-heat-collection",
            "/api/data-source-systems", "/api/meter-model");
    private static final List<String> PARAMETER_PATHS = List.of(
            "/api/calc-unit-defaults", "/api/emission-factor-selector", "/api/data-dict",
            "/api/default-factors", "/api/electricity-carbon-factors", "/api/factor-templates",
            "/api/factor-units", "/api/fossil-fuel-factors", "/api/ghg-units", "/api/config",
            "/api/thermal-emission-factors", "/api/unit-conversions", "/api/units",
            "/api/waste-incineration-factors", "/api/wastewater-treatment-factors",
            "/api/energy-categories");

    private CarbonPermissionResolver() {
    }

    public static List<String> requiredPermissions(String method, String path) {
        if ("/api/security/me".equals(path)) {
            return List.of();
        }
        boolean readOnly = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (startsWith(path, MODEL_PATHS)) {
            if (path.endsWith("/validate") && !readOnly) {
                return List.of("carbon:model:validate", "carbon:model:edit");
            }
            return List.of(readOnly ? "carbon:model:view" : "carbon:model:edit");
        }
        if (path.startsWith("/api/calc-summary")) {
            return List.of("carbon:statistics:view", "carbon:model:view");
        }
        if (startsWith(path, COLLECTION_PATHS)) {
            return List.of(readOnly ? "carbon:params:view" : "carbon:params:collection:edit",
                    readOnly ? "carbon:model:view" : "carbon:params:edit");
        }
        if (startsWith(path, PARAMETER_PATHS)) {
            return List.of(readOnly ? "carbon:params:view" : "carbon:params:edit");
        }
        if (path.startsWith("/api/emission-data")) {
            return emissionDataPermission(method, path);
        }
        return List.of(readOnly ? "carbon:model:view" : "carbon:params:edit");
    }

    private static List<String> emissionDataPermission(String method, String path) {
        if (path.endsWith("/approve") || path.endsWith("/reject")) {
            return List.of("carbon:data:review");
        }
        if (path.endsWith("/lock") || path.endsWith("/void")) {
            return List.of("carbon:data:lock");
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return List.of("carbon:data:view", "carbon:statistics:view", "carbon:report:view");
        }
        if (path.endsWith("/submit")) {
            return List.of("carbon:data:submit");
        }
        return List.of("carbon:data:edit");
    }

    private static boolean startsWith(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }
}
