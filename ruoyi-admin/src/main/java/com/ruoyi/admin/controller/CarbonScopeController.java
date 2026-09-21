package com.ruoyi.admin.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.service.ISysUserService;

@RestController
@RequestMapping("/carbon/scopes")
public class CarbonScopeController {
    private final JdbcTemplate jdbc;
    private final ISysUserService users;

    public CarbonScopeController(JdbcTemplate jdbc, ISysUserService users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @GetMapping("/options")
    @RequiresPermissions("carbon:scope:view")
    public AjaxResult options() {
        SysUser user = operator();
        boolean companyAdmin = companyAdmin(user);
        List<Map<String,Object>> companies = jdbc.queryForList(
            "select c.dept_id as deptId,d.dept_name as deptName from sys_carbon_company c join sys_dept d on d.dept_id=c.dept_id where d.del_flag='0' and d.status='0'");
        if (!companyAdmin) companies.removeIf(c -> !belongs(user.getDeptId(), number(c.get("deptId"))));
        List<Map<String,Object>> departments = jdbc.queryForList(
            "select dept_id as deptId,parent_id as parentId,dept_name as deptName,ancestors from sys_dept where del_flag='0' and status='0' order by order_num,dept_id");
        if (!companyAdmin) departments.removeIf(d -> !belongs(number(d.get("deptId")), user.getDeptId()));
        return AjaxResult.success(Map.of("companyAdmin",companyAdmin,"companies",companies,"departments",departments));
    }

    @GetMapping
    @RequiresPermissions("carbon:scope:view")
    public AjaxResult list(@RequestParam String type) {
        SysUser user = operator();
        String nameColumn = "electricity".equals(type) ? "name" : "scope_name";
        List<Map<String,Object>> scopes = jdbc.queryForList(
            "select s.id,s."+nameColumn+" as name,o.company_dept_id as companyDeptId,d.dept_name as companyName from "+table(type)+" s "
            +"left join sys_carbon_scope_owner o on o.scope_type=? and o.scope_id=s.id left join sys_dept d on d.dept_id=o.company_dept_id order by s.sort_order,s.id",type);
        if (!companyAdmin(user)) {
            if (user.getRoles().stream().anyMatch(r -> "region_admin".equals(r.getRoleKey()) && "0".equals(r.getStatus()))) {
                scopes.removeIf(s -> !belongs(user.getDeptId(),number(s.get("companyDeptId"))));
            } else {
                scopes.removeIf(s -> !userCanReadScope(user,type,number(s.get("id"))));
            }
        }
        for (Map<String,Object> scope : scopes) {
            scope.put("grants",jdbc.queryForList("select g.grant_id as grantId,g.dept_id as deptId,d.dept_name as deptName,"
                    + "g.node_type as nodeType,g.node_id as nodeId,g.node_name as nodeName,g.can_read as canRead,"
                    + "g.can_write as canWrite,g.can_delegate as canDelegate,g.granted_by_dept_id as grantedByDeptId "
                    + "from sys_carbon_scope_grant g join sys_dept d on d.dept_id=g.dept_id "
                    + "where g.scope_type=? and g.scope_id=? order by g.node_type,g.node_id,g.dept_id",type,scope.get("id")));
        }
        return AjaxResult.success(scopes);
    }

    @GetMapping("/{type}/{scopeId}/nodes")
    @RequiresPermissions("carbon:scope:view")
    public AjaxResult nodes(@PathVariable String type,@PathVariable Long scopeId) {
        validateType(type);
        List<Map<String,Object>> nodes=new ArrayList<>();
        if("electricity".equals(type)) {
            nodes.addAll(jdbc.queryForList("select id as nodeId,'station' as nodeType,name as nodeName,null as parentNodeId,null as parentNodeType from emission_station_interval where id=?",scopeId));
            nodes.addAll(jdbc.queryForList("select p.id as nodeId,'concentrator' as nodeType,p.name as nodeName,p.station_interval_id as parentNodeId,'station' as parentNodeType from emission_concentrator p where p.station_interval_id=? order by p.sort_order,p.id",scopeId));
            nodes.addAll(jdbc.queryForList("select m.id as nodeId,'meter' as nodeType,m.name as nodeName,m.point_id as parentNodeId,'concentrator' as parentNodeType from emission_meter_info m join emission_concentrator p on p.id=m.point_id where p.station_interval_id=? order by m.sort_order,m.id",scopeId));
        } else {
            String p="fossil".equals(type)?"emission_fossil_fuel":"emission_purchased_heat";
            nodes.addAll(jdbc.queryForList("select id as nodeId,'scope' as nodeType,scope_name as nodeName,null as parentNodeId,null as parentNodeType from "+p+"_collection_scope where id=?",scopeId));
            nodes.addAll(jdbc.queryForList("select s.id as nodeId,'sub_scope' as nodeType,s.sub_scope_name as nodeName,s.collection_scope_id as parentNodeId,'scope' as parentNodeType from "+p+"_collection_sub_scope s where s.collection_scope_id=? order by s.sort_order,s.id",scopeId));
            nodes.addAll(jdbc.queryForList("select m.id as nodeId,'meter' as nodeType,m.name as nodeName,m.sub_scope_id as parentNodeId,'sub_scope' as parentNodeType from "+p+"_meter_info m join "+p+"_collection_sub_scope s on s.id=m.sub_scope_id where s.collection_scope_id=? order by m.sort_order,m.id",scopeId));
        }
        for(Map<String,Object> node:nodes) node.put("grants",jdbc.queryForList("select g.grant_id as grantId,g.dept_id as deptId,d.dept_name as deptName,g.can_read as canRead,g.can_write as canWrite,g.can_delegate as canDelegate from sys_carbon_scope_grant g join sys_dept d on d.dept_id=g.dept_id where g.scope_type=? and g.scope_id=? and g.node_type=? and g.node_id=? order by g.dept_id",type,scopeId,node.get("nodeType"),node.get("nodeId")));
        return AjaxResult.success(nodes);
    }

    @PutMapping("/{type}/{id}")
    @RequiresPermissions("carbon:scope:edit")
    @Log(title="采集范围分配",businessType=BusinessType.UPDATE)
    @Transactional
    public AjaxResult save(@PathVariable String type,@PathVariable Long id,@RequestBody Assignment request) {
        SysUser user = manager();
        if (jdbc.queryForObject("select count(*) from "+table(type)+" where id=?",Integer.class,id)==0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"采集范围不存在");
        }
        if (request.companyDeptId()==null || jdbc.queryForObject("select count(*) from sys_carbon_company c join sys_dept d on d.dept_id=c.dept_id where c.dept_id=? and d.del_flag='0' and d.status='0'",Integer.class,request.companyDeptId())==0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择有效分公司");
        }
        // Lock the source row even when an ownership assignment has not been created yet.
        jdbc.queryForList("select id from "+table(type)+" where id=? for update",id);
        List<Long> owners = jdbc.queryForList("select company_dept_id from sys_carbon_scope_owner where scope_type=? and scope_id=? for update",Long.class,type,id);
        Long oldOwner = owners.isEmpty()?null:owners.get(0);
        if (!companyAdmin(user) && (!Objects.equals(oldOwner,request.companyDeptId()) || !belongs(user.getDeptId(),oldOwner))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅公司级管理员可调整公司归属");
        }
        List<Grant> grants = request.grants()==null ? List.of() : request.grants();
        for (Grant grant : grants) {
            if (!belongs(grant.deptId(),request.companyDeptId()) || Objects.equals(grant.deptId(),request.companyDeptId())
                    || (!companyAdmin(user) && !belongs(grant.deptId(),user.getDeptId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"授权部门必须在该分公司及当前管理范围内");
            }
        }
        List<Map<String,Object>> before = jdbc.queryForList("select * from sys_carbon_scope_grant where scope_type=? and scope_id=?",type,id);
        jdbc.update("insert into sys_carbon_scope_owner(scope_type,scope_id,company_dept_id,created_by,updated_by) values(?,?,?,?,?) on duplicate key update company_dept_id=values(company_dept_id),updated_by=values(updated_by)",type,id,request.companyDeptId(),user.getUserId(),user.getUserId());
        jdbc.update("delete from sys_carbon_dept_scope where scope_type=? and scope_id=?",type,id);
        jdbc.update("delete from sys_carbon_scope_grant where scope_type=? and scope_id=? and node_type=? and node_id=?",type,id,rootNodeType(type),id);
        for (Grant grant:grants) {
            jdbc.update("insert into sys_carbon_dept_scope(dept_id,scope_type,scope_id,can_write,created_by,updated_by) values(?,?,?,?,?,?)",grant.deptId(),type,id,grant.canWrite()?1:0,user.getUserId(),user.getUserId());
            jdbc.update("insert into sys_carbon_scope_grant(scope_type,scope_id,node_type,node_id,node_name,dept_id,can_read,can_write,can_delegate,granted_by_dept_id,created_by,updated_by) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                    type,id,rootNodeType(type),id,rootName(type,id),grant.deptId(),1,grant.canWrite()?1:0,1,request.companyDeptId(),user.getUserId(),user.getUserId());
        }
        return AjaxResult.success(Map.of("scopeType",type,"scopeId",id,"previousCompany",oldOwner==null?0:oldOwner,"companyDeptId",request.companyDeptId(),"before",before,"after",grants));
    }

    /** Assigns a physical child node to a department or electricity crew. */
    @PutMapping("/{type}/{scopeId}/nodes/{nodeType}/{nodeId}")
    @RequiresPermissions("carbon:scope:edit")
    @Log(title="采集节点权限委派",businessType=BusinessType.UPDATE)
    @Transactional
    public AjaxResult saveNodeGrant(@PathVariable String type,@PathVariable Long scopeId,@PathVariable String nodeType,
                                    @PathVariable Long nodeId,@RequestBody NodeAssignment request) {
        SysUser user=operator();
        validateType(type);
        Long root=scopeRoot(type,nodeType,nodeId);
        if(!Objects.equals(root,scopeId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"节点不属于当前采集范围");
        Long company=jdbc.queryForObject("select company_dept_id from sys_carbon_scope_owner where scope_type=? and scope_id=?",Long.class,type,scopeId);
        if(company==null || request.deptId()==null || !belongs(request.deptId(),company) || Objects.equals(request.deptId(),company)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"目标部门必须属于当前分公司且不能是分公司本身");
        }
        if(!companyAdmin(user) && !delegatorCanDelegate(user,type,scopeId,nodeType,nodeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"当前部门没有继续委派该节点的权限");
        }
        if(!companyAdmin(user) && !belongs(request.deptId(),user.getDeptId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只能向本部门下属部门或工队委派");
        }
        String normalized=normalizeNodeType(nodeType);
        if(!Boolean.TRUE.equals(request.canRead())) {
            jdbc.update("delete from sys_carbon_scope_grant where dept_id=? and scope_type=? and scope_id=? and node_type=? and node_id=?",
                    request.deptId(),type,scopeId,normalized,nodeId);
        } else {
            jdbc.update("insert into sys_carbon_scope_grant(scope_type,scope_id,node_type,node_id,node_name,dept_id,can_read,can_write,can_delegate,granted_by_dept_id,created_by,updated_by) "
                            + "values(?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update node_name=values(node_name),can_read=values(can_read),can_write=values(can_write),can_delegate=values(can_delegate),granted_by_dept_id=values(granted_by_dept_id),updated_by=values(updated_by)",
                    type,scopeId,normalized,nodeId,nodeName(type,normalized,nodeId),request.deptId(),1,Boolean.TRUE.equals(request.canWrite())?1:0,
                    Boolean.TRUE.equals(request.canDelegate())?1:0,user.getDeptId(),user.getUserId(),user.getUserId());
        }
        return AjaxResult.success(jdbc.queryForList("select g.grant_id as grantId,g.dept_id as deptId,d.dept_name as deptName,g.node_type as nodeType,g.node_id as nodeId,g.node_name as nodeName,g.can_read as canRead,g.can_write as canWrite,g.can_delegate as canDelegate,g.granted_by_dept_id as grantedByDeptId from sys_carbon_scope_grant g join sys_dept d on d.dept_id=g.dept_id where g.scope_type=? and g.scope_id=? order by g.node_type,g.node_id,g.dept_id",type,scopeId));
    }

    private SysUser manager() {
        SysUser user = users.selectUserById(SecurityUtils.getUserId());
        if (user==null || !"0".equals(user.getStatus()) || (!companyAdmin(user) && (user.getRoles().stream().noneMatch(r -> "region_admin".equals(r.getRoleKey()) && "0".equals(r.getStatus()))
                || user.getDeptId()==null || jdbc.queryForObject("select count(*) from sys_carbon_company where dept_id=?",Integer.class,user.getDeptId())==0))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅公司级或地区管理员可分配范围");
        }
        return user;
    }

    private SysUser operator() {
        SysUser user=users.selectUserById(SecurityUtils.getUserId());
        if(user==null || !"0".equals(user.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"当前用户不可用");
        return user;
    }

    private boolean companyAdmin(SysUser user) {
        return user.isAdmin() || user.getRoles().stream().anyMatch(r -> "company_admin".equals(r.getRoleKey()) && "0".equals(r.getStatus()));
    }

    private boolean belongs(Long deptId,Long ancestorId) {
        if (deptId==null || ancestorId==null) return false;
        return jdbc.queryForObject("select count(*) from sys_dept where dept_id=? and del_flag='0' and status='0' and (dept_id=? or find_in_set(?,ancestors)>0)",Integer.class,deptId,ancestorId,ancestorId)>0;
    }

    private boolean userCanReadScope(SysUser user,String type,Long scopeId) {
        if(user.getDeptId()==null || scopeId==null) return false;
        return jdbc.queryForObject("select count(*) from sys_carbon_scope_grant g join sys_dept d on d.dept_id=g.dept_id "
                + "join sys_dept u on u.dept_id=? where g.scope_type=? and g.scope_id=? and g.can_read=1 "
                + "and (g.dept_id=u.dept_id or find_in_set(g.dept_id,u.ancestors)>0) "
                + "and d.status='0' and d.del_flag='0'",Integer.class,user.getDeptId(),type,scopeId)>0;
    }

    private Long number(Object value) { return value instanceof Number n?n.longValue():null; }
    private void validateType(String type) { table(type); }
    private String rootNodeType(String type) { return "electricity".equals(type)?"station":"scope"; }
    private String normalizeNodeType(String type) { return switch(type) { case "root"->"scope"; case "sub-scope","subScope"->"sub_scope"; case "point"->"concentrator"; default->type; }; }
    private Long scopeRoot(String type,String nodeType,Long nodeId) {
        String n=normalizeNodeType(nodeType);
        String sql;
        if("electricity".equals(type)) sql=switch(n) {
            case "station","scope"->"select id from emission_station_interval where id=?";
            case "concentrator"->"select station_interval_id from emission_concentrator where id=?";
            case "meter"->"select p.station_interval_id from emission_meter_info m join emission_concentrator p on p.id=m.point_id where m.id=?";
            default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"无效电力节点类型");
        }; else { String p="fossil".equals(type)?"emission_fossil_fuel":"emission_purchased_heat"; sql=switch(n) {
            case "scope"->"select id from "+p+"_collection_scope where id=?";
            case "sub_scope"->"select collection_scope_id from "+p+"_collection_sub_scope where id=?";
            case "meter"->"select s.collection_scope_id from "+p+"_meter_info m join "+p+"_collection_sub_scope s on s.id=m.sub_scope_id where m.id=?";
            default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"无效采集节点类型");
        }; }
        List<Long> roots=jdbc.queryForList(sql,Long.class,nodeId); if(roots.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"节点不存在"); return roots.get(0);
    }
    private String rootName(String type,Long id) { return nodeName(type,rootNodeType(type),id); }
    private String nodeName(String type,String nodeType,Long id) {
        String n=normalizeNodeType(nodeType),sql=switch(type) {
            case "electricity"->switch(n) { case "station","scope"->"select name from emission_station_interval where id=?"; default->null; };
            case "fossil"->switch(n) { case "scope"->"select scope_name from emission_fossil_fuel_collection_scope where id=?"; case "sub_scope"->"select sub_scope_name from emission_fossil_fuel_collection_sub_scope where id=?"; case "meter"->"select name from emission_fossil_fuel_meter_info where id=?"; default->null; };
            case "heat"->switch(n) { case "scope"->"select scope_name from emission_purchased_heat_collection_scope where id=?"; case "sub_scope"->"select sub_scope_name from emission_purchased_heat_collection_sub_scope where id=?"; case "meter"->"select name from emission_purchased_heat_meter_info where id=?"; default->null; };
            default->null; };
        if(sql==null) return null; List<String> names=jdbc.queryForList(sql,String.class,id); return names.isEmpty()?null:names.get(0);
    }
    private boolean delegatorCanDelegate(SysUser user,String type,Long scopeId,String nodeType,Long nodeId) {
        if(user.getDeptId()==null) return false;
        String n=normalizeNodeType(nodeType);
        List<Map<String,Object>> grants=jdbc.queryForList("select node_type,node_id from sys_carbon_scope_grant where scope_type=? and scope_id=? and dept_id=? and can_delegate=1",type,scopeId,user.getDeptId());
        for(Map<String,Object> grant:grants) if(grantCovers(type,String.valueOf(grant.get("node_type")),number(grant.get("node_id")),n,nodeId)) return true;
        return false;
    }
    private boolean grantCovers(String type,String grantType,Long grantId,String requestedType,Long requestedId) {
        String g=normalizeNodeType(grantType); if("electricity".equals(type)) {
            if(g.equals("station")||g.equals("scope")) return true;
            if(g.equals("concentrator")) { Long p=requestedType.equals("meter")?jdbc.queryForObject("select point_id from emission_meter_info where id=?",Long.class,requestedId):requestedId; return Objects.equals(g,requestedType)?Objects.equals(grantId,requestedId):Objects.equals(grantId,p); }
            return g.equals("meter")&&Objects.equals(grantId,requestedId);
        }
        if(g.equals("scope")) return true; if(g.equals("sub_scope")) { Long s=requestedType.equals("meter")?jdbc.queryForObject("select sub_scope_id from "+("fossil".equals(type)?"emission_fossil_fuel_meter_info":"emission_purchased_heat_meter_info")+" where id=?",Long.class,requestedId):requestedId; return Objects.equals(grantId,s); }
        return g.equals("meter")&&Objects.equals(grantId,requestedId);
    }
    private String table(String type) {
        return switch(type) {
            case "electricity" -> "emission_station_interval";
            case "fossil" -> "emission_fossil_fuel_collection_scope";
            case "heat" -> "emission_purchased_heat_collection_scope";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"无效范围类型");
        };
    }
    public record Grant(Long deptId,boolean canWrite) {}
    public record Assignment(Long companyDeptId,List<Grant> grants) {}
    public record NodeAssignment(Long deptId,Boolean canRead,Boolean canWrite,Boolean canDelegate) {}
}
