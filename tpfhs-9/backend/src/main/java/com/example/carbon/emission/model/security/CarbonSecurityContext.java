package com.example.carbon.emission.model.security;

import java.util.List;
import java.util.Set;

public record CarbonSecurityContext(
        Long userId,
        String userName,
        String nickName,
        Long deptId,
        String deptName,
        Set<String> roles,
        Set<String> permissions,
        boolean allData,
        boolean selfOnly,
        List<Long> deptIds,
        List<Long> nodeIds) {

    public static final String REQUEST_ATTRIBUTE = CarbonSecurityContext.class.getName();

    public boolean hasPermission(String required) {
        if (required == null || required.isBlank()) {
            return true;
        }
        return permissions != null && permissions.stream().anyMatch(granted -> matches(granted, required));
    }

    private boolean matches(String granted, String required) {
        if ("*:*:*".equals(granted) || required.equals(granted)) {
            return true;
        }
        if (granted == null || !granted.contains("*")) {
            return false;
        }
        String regex = java.util.regex.Pattern.quote(granted).replace("*", "\\E.*\\Q");
        return required.matches(regex);
    }
}
