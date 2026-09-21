package com.example.carbon.emission.model.security;

import java.lang.reflect.Method;
import java.util.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

/** All collection entry points share the same physical-scope checks. */
@Aspect
@Component
public class CollectionScopeAspect {
    private final CollectionScopeService scopes;
    private final ObjectMapper mapper;
    public CollectionScopeAspect(CollectionScopeService scopes,ObjectMapper mapper) {
        this.scopes=scopes; this.mapper=mapper;
    }

    @Around("execution(public * com.example.carbon.emission.model.controller.MeterSettingsController.*(..)) || execution(public * com.example.carbon.emission.model.controller.FossilFuelCollectionController.*(..)) || execution(public * com.example.carbon.emission.model.controller.PurchasedHeatCollectionController.*(..))")
    public Object check(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        String type=point.getTarget().getClass().getSimpleName().startsWith("MeterSettings")?"electricity":point.getTarget().getClass().getSimpleName().startsWith("Fossil")?"fossil":"heat";
        Method method=((MethodSignature)point.getSignature()).getMethod();
        String name=method.getName();
        boolean write=!Set.of("GET","HEAD").contains(request.getMethod());
        @SuppressWarnings("unchecked")
        Map<String,String> variables=(Map<String,String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if(variables==null) variables=Map.of();
        Map<String,Object> body=Map.of();
        for(int i=0;i<method.getParameterCount();i++) {
            if(method.getParameters()[i].isAnnotationPresent(RequestBody.class) && point.getArgs()[i]!=null) body=map(point.getArgs()[i]);
        }
        String nodeType=variables.get("nodeType");
        if(nodeType==null && body.get("nodeType")!=null) nodeType=String.valueOf(body.get("nodeType"));
        if(nodeType==null) nodeType=name.toLowerCase(Locale.ROOT).contains("subscope")?"sub_scope":name.toLowerCase(Locale.ROOT).contains("scope")?"scope":name.toLowerCase(Locale.ROOT).contains("concentrator")?"concentrator":"meter";
        nodeType=normalize(nodeType);
        if(variables.containsKey("id")) {
            if(write && Set.of("scope","station").contains(nodeType)) scopes.requireCompanyAdmin();
            scopes.require(type,nodeType,number(variables.get("id")),write);
        }
        checkReferences(type,variables,write);
        Map<String,Object> query=new LinkedHashMap<>();
        for(String key:List.of("stationId","pointId","subScopeId","excludeMeterId")) {
            if(request.getParameter(key)!=null) query.put(key,request.getParameter(key));
        }
        checkReferences(type,query,write);
        if(write) {
            if(Set.of("createScope","createStation","testMeterReadingWithoutId").contains(name)) scopes.requireCompanyAdmin();
            if(name.startsWith("create") && number(body.get("id"))!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"新建节点不能指定已有ID");
            checkReferences(type,body,true);
            if(name.equals("createConcentrator") && number(body.get("stationIntervalId"))==null) missingParent();
            if(name.equals("createMeter") && number(body.get("electricity".equals(type)?"pointId":"subScopeId"))==null) missingParent();
            if(name.equals("createSubScope") && number(body.get("collectionScopeId"))==null) missingParent();
            if(!"electricity".equals(type) && number(body.get("parentMeterId"))!=null) {
                Long parentRoot=scopes.scopeId(type,"meter",number(body.get("parentMeterId")));
                Long targetRoot=number(body.get("subScopeId"))!=null?scopes.scopeId(type,"sub_scope",number(body.get("subScopeId"))):variables.containsKey("id")?scopes.scopeId(type,"meter",number(variables.get("id"))):null;
                if(!Objects.equals(parentRoot,targetRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"父子采集点必须属于同一采集范围");
            }
        }
        Object result=point.proceed();
        if (name.startsWith("delete") && Set.of("scope","station").contains(nodeType) && variables.containsKey("id")) {
            scopes.removeDeletedScope(type,number(variables.get("id")));
        }
        if(write || !(result instanceof ResponseEntity<?> response) || !response.getStatusCode().is2xxSuccessful()) return result;
        Object data=response.getBody();
        if("getTree".equals(name) && data!=null) {
            Map<String,Object> root=map(data);
            root.put("children",filterChildren(type,root.get("children")));
            return new ResponseEntity<>(root,response.getHeaders(),response.getStatusCode());
        }
        if(data instanceof List<?> list) {
            String itemType=itemType(name);
            List<?> filtered=list.stream().filter(item -> {
                Long id=number(map(item).get("id"));
                return id!=null && scopes.isAllowed(type,itemType,id,false);
            }).toList();
            return new ResponseEntity<>(filtered,response.getHeaders(),response.getStatusCode());
        }
        return result;
    }

    private List<Map<String,Object>> filterChildren(String type,Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String,Object>> result=new ArrayList<>();
        for(Object item:list) {
            if (!(item instanceof Map<?,?>)) continue;
            Map<String,Object> node=filterNode(type,map(item));
            if(node!=null) result.add(node);
        }
        return result;
    }

    private Map<String,Object> filterNode(String type,Map<String,Object> node) {
        String nodeType=node.get("nodeType")==null?"root":String.valueOf(node.get("nodeType"));
        Long id=number(node.get("id"));
        List<Map<String,Object>> children=filterChildren(type,node.get("children"));
        boolean visible="root".equals(nodeType) || (id!=null && scopes.isAllowed(type,nodeType,id,false));
        if(!visible && children.isEmpty()) return null;
        node.put("children",children);
        return node;
    }

    private String itemType(String name) {
        String lower=name.toLowerCase(Locale.ROOT);
        if(name.equals("getAllScopes")) return "scope";
        if(lower.contains("subscope")) return "sub_scope";
        if(lower.contains("concentrator")) return "concentrator";
        if(lower.contains("station")) return "station";
        return "meter";
    }

    private void checkReferences(String type,Map<?,?> values,boolean write) {
        Map<String,String> references=Map.of("scopeId","scope","collectionScopeId","scope","subScopeId","sub_scope","stationId","station","stationIntervalId","station","pointId","concentrator","parentMeterId","meter","excludeMeterId","meter");
        for(Map.Entry<String,String> reference:references.entrySet()) {
            Long id=number(values.get(reference.getKey()));
            if(id!=null) scopes.require(type,reference.getValue(),id,write);
        }
    }
    private void missingParent() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择有效的所属采集范围"); }
    private String normalize(String type) { return switch(type) {case "sub-scope","subScope"->"sub_scope";case "scopes"->"scope";case "meters"->"meter";default->type;}; }
    private Map<String,Object> map(Object value) { return mapper.convertValue(value,new TypeReference<>(){}); }
    private Long number(Object value) {
        if(value==null) return null;
        try { long id=Long.parseLong(value.toString()); return id>0?id:null; } catch(NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"无效节点ID"); }
    }
}
