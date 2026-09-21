package com.example.carbon.emission.model.security;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

public class CollectionScopeServiceTest {
    private JdbcTemplate jdbc;
    private CollectionScopeService scopes;
    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("create alias FIND_IN_SET for 'com.example.carbon.emission.model.security.CollectionScopeServiceTest.findInSet'");
        jdbc.execute("create table sys_dept(dept_id bigint,parent_id bigint,ancestors varchar(100),status varchar(1),del_flag varchar(1))");
        jdbc.execute("create table sys_carbon_company(dept_id bigint)");
        jdbc.execute("create table sys_carbon_scope_owner(scope_type varchar(20),scope_id bigint,company_dept_id bigint)");
        jdbc.execute("create table sys_carbon_dept_scope(dept_id bigint,scope_type varchar(20),scope_id bigint,can_write int)");
        jdbc.execute("create table sys_carbon_scope_grant(grant_id bigint auto_increment primary key,scope_type varchar(20),scope_id bigint,node_type varchar(30),node_id bigint,node_name varchar(200),dept_id bigint,can_read int,can_write int,can_delegate int)");
        jdbc.execute("insert into sys_dept values(1,0,'0','0','0'),(10,1,'0,1','0','0'),(20,1,'0,1','0','0'),(11,10,'0,1,10','0','0'),(12,11,'0,1,10,11','0','0'),(21,20,'0,1,20','0','0')");
        jdbc.execute("insert into sys_carbon_company values(10),(20)");
        jdbc.execute("insert into sys_carbon_scope_owner values('electricity',1,10),('electricity',2,20),('fossil',1,10),('heat',1,20)");
        jdbc.execute("create table emission_station_interval(id bigint)");
        jdbc.execute("insert into emission_station_interval values(1),(2),(3)");
        jdbc.execute("create table emission_concentrator(id bigint,station_interval_id bigint)");
        jdbc.execute("insert into emission_concentrator values(5,1),(6,2)");
        jdbc.execute("create table emission_meter_info(id bigint,point_id bigint)");
        jdbc.execute("insert into emission_meter_info values(90,5),(91,6)");
        for(String prefix:List.of("emission_fossil_fuel","emission_purchased_heat")) {
            jdbc.execute("create table "+prefix+"_collection_scope(id bigint)");
            jdbc.execute("create table "+prefix+"_collection_sub_scope(id bigint,collection_scope_id bigint)");
            jdbc.execute("create table "+prefix+"_meter_info(id bigint,sub_scope_id bigint)");
            jdbc.execute("insert into "+prefix+"_collection_scope values(1)");
            jdbc.execute("insert into "+prefix+"_collection_sub_scope values(5,1)");
            jdbc.execute("insert into "+prefix+"_meter_info values(90,5)");
        }
        scopes=new CollectionScopeService(jdbc);
        identity(12,List.of(12L),false,Set.of("carbon_data_entry"));
    }
    @AfterEach void cleanup() { jdbc.execute("shutdown"); RequestContextHolder.resetRequestAttributes(); }
    public static int findInSet(Long id,String list) { return list!=null && Arrays.asList(list.split(",")).contains(String.valueOf(id))?1:0; }
    private void identity(long dept,List<Long> departments,boolean all,Set<String> roles) {
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setAttribute(CarbonSecurityContext.REQUEST_ATTRIBUTE,new CarbonSecurityContext(100L,"test","test",dept,"department",roles,Set.of(),all,false,departments,List.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @Test void noAssignmentDeniesAllPhysicalScopes() {
        assertThat(scopes.allowed("electricity",false)).isEmpty();
        assertThatThrownBy(()->scopes.require("electricity","meter",90L,false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void operationalGroupGrantIsInheritedByCrew() {
        grant(11,"electricity",1,"station",1,true,true,true);
        assertThat(scopes.allowed("electricity",true)).containsExactly(1L);
        scopes.require("electricity","meter",90L,true);
        assertThatThrownBy(()->scopes.require("electricity","meter",91L,true)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void readOnlyGrantCannotWrite() {
        grant(12,"electricity",1,"station",1,true,false,false);
        assertThat(scopes.allowed("electricity",false)).containsExactly(1L);
        assertThat(scopes.allowed("electricity",true)).isEmpty();
    }
    @Test void companyScopeDoesNotGiveEveryEmployeeAccess() {
        grant(10,"electricity",1,"station",1,true,true,true);
        assertThat(scopes.allowed("electricity",false)).isEmpty();
    }
    @Test void regionalAdministratorCanOnlyUseTheirCompany() {
        identity(10,List.of(10L,11L,12L),false,Set.of("region_admin"));
        assertThat(scopes.allowed("electricity",true)).containsExactly(1L);
        assertThat(scopes.allowed("heat",false)).isEmpty();
    }
    @Test void revokedAndDisabledDepartmentsLoseAccess() {
        grant(11,"electricity",1,"station",1,true,true,true);
        jdbc.execute("update sys_dept set status='1' where dept_id=11");
        assertThat(scopes.allowed("electricity",false)).isEmpty();
    }
    @Test void reassignedCompanyCannotRetainOldDepartmentGrant() {
        grant(11,"electricity",1,"station",1,true,true,true);
        jdbc.execute("update sys_carbon_scope_owner set company_dept_id=20 where scope_type='electricity' and scope_id=1");
        assertThat(scopes.allowed("electricity",false)).isEmpty();
    }
    @Test void sameMeterIdInDifferentTablesDoesNotSharePermissions() {
        grant(11,"fossil",1,"scope",1,true,true,true);
        scopes.requireCollectionPoint(2,90L,true);
        assertThatThrownBy(()->scopes.requireCollectionPoint(1,90L,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->scopes.requireCollectionPoint(3,90L,false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void invalidTypeAndMissingMeterAreRejected() {
        assertThatThrownBy(()->scopes.requireCollectionPoint(99,90L,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->scopes.scopeId("electricity","meter",999L)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void allDataAuditorCannotManageRootScopes() {
        identity(1,List.of(),true,Set.of("carbon_auditor"));
        assertThat(scopes.allowed("electricity",false)).containsExactly(1L,2L,3L);
        assertThatThrownBy(scopes::requireCompanyAdmin).isInstanceOf(ResponseStatusException.class);
    }
    @Test void companyAdminCanManageRootScopes() {
        identity(1,List.of(),true,Set.of("company_admin"));
        scopes.requireCompanyAdmin();
    }
    @Test void mixedCompanySnapshotSubtotalIsNotExposed() {
        grant(11,"electricity",1,"station",1,true,true,true);
        jdbc.execute("create table emission_calculation_node(id bigint,parent_id bigint,node_id bigint,calculation_template_id bigint)");
        jdbc.execute("insert into emission_calculation_node values(1,null,100,9),(2,1,101,9),(3,1,102,9)");
        jdbc.execute("create table emission_collection_node_data(calculation_node_id bigint,collection_point_type int,collection_point_id bigint)");
        jdbc.execute("insert into emission_collection_node_data values(2,1,90),(3,1,91)");
        CarbonDataScopeService nodeScope=org.mockito.Mockito.mock(CarbonDataScopeService.class);
        SummaryScopeService summaryScope=new SummaryScopeService(jdbc,nodeScope,scopes);
        var root=new com.example.carbon.emission.model.entity.CalcNodeSummary();
        root.setCalculationNodeId(1L);root.setCalculationTemplateId(9L);root.setSourceNodeId(100L);
        var child=new com.example.carbon.emission.model.entity.CalcNodeSummary();
        child.setCalculationNodeId(2L);child.setCalculationTemplateId(9L);child.setSourceNodeId(101L);
        assertThat(summaryScope.filter(List.of(root,child))).containsExactly(child);
    }

    private void grant(long dept,String type,long scope,String nodeType,long nodeId,boolean read,boolean write,boolean delegate) {
        jdbc.update("insert into sys_carbon_scope_grant(scope_type,scope_id,node_type,node_id,dept_id,can_read,can_write,can_delegate) values(?,?,?,?,?,?,?,?)",
                type,scope,nodeType,nodeId,dept,read?1:0,write?1:0,delegate?1:0);
    }

    @Test void childGrantOnlyExposesAuthorizedPhysicalBranch() {
        grant(11,"electricity",1,"concentrator",5,true,true,true);
        scopes.require("electricity","concentrator",5L,false);
        scopes.require("electricity","meter",90L,true);
        assertThatThrownBy(() -> scopes.require("electricity","meter",91L,false)).isInstanceOf(ResponseStatusException.class);
        assertThat(scopes.isAllowed("electricity","station",1L,false)).isFalse();
    }
}
