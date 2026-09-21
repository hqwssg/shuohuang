package com.example.carbon.emission.model.security;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Resolves physical collection-node permissions. */
@Service
public class CollectionScopeService {
    private final JdbcTemplate jdbc;

    public CollectionScopeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<Long> allowed(String type, boolean write) {
        CarbonSecurityContext context = CarbonSecurityContext.current();
        List<Long> roots = jdbc.queryForList("select id from " + scopeTable(type) + " order by id", Long.class);
        if (context.allData()) return new LinkedHashSet<>(roots);
        Set<Long> result = new LinkedHashSet<>();
        for (Long root : roots) {
            if (isAllowed(type, rootNodeType(type), root, write)) result.add(root);
        }
        return result;
    }

    public boolean isAllowed(String type, String nodeType, Long id, boolean write) {
        Long root = scopeId(type, nodeType, id);
        CarbonSecurityContext context = CarbonSecurityContext.current();
        if (context.allData()) return true;
        if (managerOwnsRoot(context, type, root)) return true;
        Set<Long> departments = effectiveDepartmentIds(context);
        if (departments.isEmpty()) return false;
        String placeholders = departments.stream().map(value -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>();
        args.add(type); args.add(root); args.addAll(departments);
        String permission = write ? " and g.can_write=1" : " and g.can_read=1";
        List<Map<String, Object>> grants = jdbc.queryForList(
                "select g.node_type,g.node_id from sys_carbon_scope_grant g "
                        + "join sys_carbon_scope_owner o on o.scope_type=g.scope_type and o.scope_id=g.scope_id "
                        + "join sys_dept d on d.dept_id=g.dept_id "
                        + "where g.scope_type=? and g.scope_id=? and g.dept_id in (" + placeholders + ") "
                        + "and d.status='0' and d.del_flag='0' "
                        + "and (d.dept_id=o.company_dept_id or find_in_set(o.company_dept_id,d.ancestors)>0)"
                        + permission, args.toArray());
        for (Map<String, Object> grant : grants) {
            if (grantApplies(type, text(grant.get("node_type")), number(grant.get("node_id")), nodeType, id)) return true;
        }
        return false;
    }

    public void require(String type, String nodeType, Long id, boolean write) {
        scopeId(type, nodeType, id);
        if (!isAllowed(type, nodeType, id, write)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "当前部门没有该采集节点的" + (write ? "维护" : "读取") + "权限");
        }
    }

    public boolean canDelegate(String type, String nodeType, Long id) {
        CarbonSecurityContext context = CarbonSecurityContext.current();
        Long root = scopeId(type, nodeType, id);
        if (context.allData() || hasRole(context, "company_admin")) return true;
        if (hasRole(context, "region_admin") && managerOwnsRoot(context, type, root)) return true;
        Set<Long> departments = effectiveDepartmentIds(context);
        if (departments.isEmpty()) return false;
        String placeholders = departments.stream().map(value -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(); args.add(type); args.add(root); args.addAll(departments);
        List<Map<String, Object>> grants = jdbc.queryForList(
                "select g.node_type,g.node_id from sys_carbon_scope_grant g join sys_dept d on d.dept_id=g.dept_id "
                        + "where g.scope_type=? and g.scope_id=? and g.dept_id in (" + placeholders + ") "
                        + "and g.can_delegate=1 and d.status='0' and d.del_flag='0'", args.toArray());
        return grants.stream().anyMatch(grant -> grantApplies(type, text(grant.get("node_type")), number(grant.get("node_id")), nodeType, id));
    }

    public Long scopeId(String type, String nodeType, Long id) {
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少有效采集节点ID");
        String sql;
        if ("electricity".equals(type)) {
            sql = switch (normalize(nodeType)) {
                case "station", "scope" -> "select id from emission_station_interval where id=?";
                case "concentrator", "point" -> "select station_interval_id from emission_concentrator where id=?";
                case "meter" -> "select p.station_interval_id from emission_meter_info m join emission_concentrator p on p.id=m.point_id where m.id=?";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效电力节点类型");
            };
        } else {
            String prefix = switch (type) {
                case "fossil" -> "emission_fossil_fuel";
                case "heat" -> "emission_purchased_heat";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效范围类型");
            };
            sql = switch (normalize(nodeType)) {
                case "scope" -> "select id from " + prefix + "_collection_scope where id=?";
                case "sub_scope" -> "select collection_scope_id from " + prefix + "_collection_sub_scope where id=?";
                case "meter" -> "select s.collection_scope_id from " + prefix + "_meter_info m join "
                        + prefix + "_collection_sub_scope s on s.id=m.sub_scope_id where m.id=?";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效采集节点类型");
            };
        }
        List<Long> roots = jdbc.queryForList(sql, Long.class, id);
        if (roots.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "采集节点不存在");
        return roots.get(0);
    }

    public String nodeName(String type, String nodeType, Long id) {
        String normalized = normalize(nodeType);
        String sql = switch (type) {
            case "electricity" -> switch (normalized) {
                case "station", "scope" -> "select name from emission_station_interval where id=?";
                case "concentrator", "point" -> "select name from emission_concentrator where id=?";
                case "meter" -> "select name from emission_meter_info where id=?";
                default -> null;
            };
            case "fossil" -> switch (normalized) {
                case "scope" -> "select scope_name from emission_fossil_fuel_collection_scope where id=?";
                case "sub_scope" -> "select sub_scope_name from emission_fossil_fuel_collection_sub_scope where id=?";
                case "meter" -> "select name from emission_fossil_fuel_meter_info where id=?";
                default -> null;
            };
            case "heat" -> switch (normalized) {
                case "scope" -> "select scope_name from emission_purchased_heat_collection_scope where id=?";
                case "sub_scope" -> "select sub_scope_name from emission_purchased_heat_collection_sub_scope where id=?";
                case "meter" -> "select name from emission_purchased_heat_meter_info where id=?";
                default -> null;
            };
            default -> null;
        };
        if (sql == null) return null;
        List<String> names = jdbc.queryForList(sql, String.class, id);
        return names.isEmpty() ? null : names.get(0);
    }

    public void requireCollectionPoint(Integer type, Long id, boolean write) {
        String scopeType = switch (type == null ? 0 : type) {
            case 1 -> "electricity"; case 2 -> "fossil"; case 3 -> "heat";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效采集点类型");
        };
        require(scopeType, "meter", id, write);
    }

    public void requireCompanyAdmin() {
        CarbonSecurityContext context = CarbonSecurityContext.current();
        if (!hasRole(context, "company_admin") && !context.permissions().contains("*:*:*")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅公司级管理员可维护一级采集范围");
        }
    }

    public void removeDeletedScope(String type, Long id) {
        if (jdbc.queryForObject("select count(*) from " + scopeTable(type) + " where id=?", Integer.class, id) == 0) {
            jdbc.update("delete from sys_carbon_scope_owner where scope_type=? and scope_id=?", type, id);
            jdbc.update("delete from sys_carbon_scope_grant where scope_type=? and scope_id=?", type, id);
        }
    }

    public String normalize(String nodeType) {
        return switch (nodeType == null ? "" : nodeType) {
            case "root" -> "scope";
            case "sub-scope", "subScope" -> "sub_scope";
            case "scopes" -> "scope";
            case "meters" -> "meter";
            default -> nodeType;
        };
    }

    private boolean grantApplies(String type, String grantType, Long grantId, String requestedType, Long requestedId) {
        String grant = normalize(grantType), requested = normalize(requestedType);
        if (grantId == null || requestedId == null) return false;
        if ("electricity".equals(type)) {
            if ("station".equals(grant) || "scope".equals(grant)) return true;
            if ("concentrator".equals(grant)) {
                Long point = "concentrator".equals(requested) || "point".equals(requested)
                        ? requestedId : parentPointId(requestedId, requested);
                return Objects.equals(grantId, point);
            }
            return "meter".equals(grant) && isMeterDescendant("electricity", grantId, requestedId, requested);
        }
        if ("scope".equals(grant)) return true;
        if ("sub_scope".equals(grant)) {
            Long subScope = "sub_scope".equals(requested) ? requestedId : parentSubScopeId(type, requestedId, requested);
            return Objects.equals(grantId, subScope);
        }
        return "meter".equals(grant) && isMeterDescendant(type, grantId, requestedId, requested);
    }

    private boolean isMeterDescendant(String type, Long grantMeter, Long requestedId, String requestedType) {
        if (!"meter".equals(requestedType)) return false;
        String table = "electricity".equals(type) ? "emission_meter_info"
                : "fossil".equals(type) ? "emission_fossil_fuel_meter_info" : "emission_purchased_heat_meter_info";
        Long current = requestedId;
        for (int i = 0; i < 100 && current != null; i++) {
            if (Objects.equals(grantMeter, current)) return true;
            List<Long> parents = jdbc.queryForList("select parent_meter_id from " + table + " where id=?", Long.class, current);
            current = parents.isEmpty() ? null : parents.get(0);
            if (Objects.equals(current, 0L)) current = null;
        }
        return false;
    }

    private Long parentPointId(Long id, String requestedType) {
        if ("meter".equals(requestedType)) {
            List<Long> points = jdbc.queryForList("select point_id from emission_meter_info where id=?", Long.class, id);
            return points.isEmpty() ? null : points.get(0);
        }
        return "concentrator".equals(requestedType) || "point".equals(requestedType) ? id : null;
    }

    private Long parentSubScopeId(String type, Long id, String requestedType) {
        if ("meter".equals(requestedType)) {
            String table = "fossil".equals(type) ? "emission_fossil_fuel_meter_info" : "emission_purchased_heat_meter_info";
            List<Long> values = jdbc.queryForList("select sub_scope_id from " + table + " where id=?", Long.class, id);
            return values.isEmpty() ? null : values.get(0);
        }
        return "sub_scope".equals(requestedType) ? id : null;
    }

    private boolean managerOwnsRoot(CarbonSecurityContext context, String type, Long root) {
        if (!hasRole(context, "company_admin") && !hasRole(context, "region_admin")) return false;
        if (context.deptIds() == null || context.deptIds().isEmpty()) return false;
        String placeholders = context.deptIds().stream().map(value -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(); args.add(type); args.add(root); args.addAll(context.deptIds());
        return !jdbc.queryForList("select o.scope_id from sys_carbon_scope_owner o where o.scope_type=? and o.scope_id=? "
                + "and o.company_dept_id in (" + placeholders + ")", Long.class, args.toArray()).isEmpty();
    }

    private Set<Long> effectiveDepartmentIds(CarbonSecurityContext context) {
        Set<Long> ids = new LinkedHashSet<>();
        if (context.deptIds() != null) ids.addAll(context.deptIds());
        if (context.deptId() != null) {
            ids.addAll(jdbc.queryForList("select p.dept_id from sys_dept u join sys_dept p "
                    + "on find_in_set(p.dept_id,u.ancestors)>0 left join sys_carbon_company c on c.dept_id=p.dept_id "
                    + "where u.dept_id=? and p.status='0' and p.del_flag='0' and c.dept_id is null",
                    Long.class, context.deptId()));
        }
        return ids;
    }

    private boolean hasRole(CarbonSecurityContext context, String role) {
        return context.roles() != null && context.roles().contains(role);
    }

    private String rootNodeType(String type) { return "electricity".equals(type) ? "station" : "scope"; }

    private String scopeTable(String type) {
        return switch (type) {
            case "electricity" -> "emission_station_interval";
            case "fossil" -> "emission_fossil_fuel_collection_scope";
            case "heat" -> "emission_purchased_heat_collection_scope";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效范围类型");
        };
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }

    private Long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException ignored) { return null; }
    }
}
