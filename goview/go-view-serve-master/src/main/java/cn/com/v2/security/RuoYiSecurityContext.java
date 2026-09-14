package cn.com.v2.security;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RuoYiSecurityContext
{
    public static final String REQUEST_ATTRIBUTE = RuoYiSecurityContext.class.getName();

    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private Set<String> permissions = Collections.emptySet();
    private boolean allData;
    private boolean selfOnly;
    private List<Long> deptIds = Collections.emptyList();

    public boolean hasPermission(String required)
    {
        if (permissions == null) return false;
        for (String granted : permissions)
        {
            if ("*:*:*".equals(granted) || required.equals(granted)) return true;
            if (granted != null && granted.endsWith("*")
                    && required.startsWith(granted.substring(0, granted.length() - 1))) return true;
        }
        return false;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    public boolean isAllData() { return allData; }
    public void setAllData(boolean allData) { this.allData = allData; }
    public boolean isSelfOnly() { return selfOnly; }
    public void setSelfOnly(boolean selfOnly) { this.selfOnly = selfOnly; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
}
