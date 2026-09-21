package com.example.carbon.emission.model.security;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.carbon.emission.model.entity.CalcNodeSummary;

@Service
public class SummaryScopeService {
    private final JdbcTemplate jdbc;
    private final CarbonDataScopeService nodes;
    private final CollectionScopeService collections;
    public SummaryScopeService(JdbcTemplate jdbc,CarbonDataScopeService nodes,CollectionScopeService collections) {
        this.jdbc=jdbc; this.nodes=nodes; this.collections=collections;
    }
    public List<CalcNodeSummary> filter(List<CalcNodeSummary> summaries) {
        if(CarbonSecurityContext.current().allData()) return summaries;
        Map<Long,Boolean> cache=new HashMap<>();
        return summaries.stream().filter(s->cache.computeIfAbsent(s.getCalculationNodeId(),id->canRead(s))).toList();
    }
    private boolean canRead(CalcNodeSummary summary) {
        if(summary.getSourceNodeId()==null) return false;
        try {
            nodes.requireNode(summary.getSourceNodeId(),false);
            List<Map<String,Object>> snapshot=jdbc.queryForList("select id,parent_id,node_id from emission_calculation_node where calculation_template_id=?",summary.getCalculationTemplateId());
            Set<Long> descendants=new LinkedHashSet<>();
            descendants.add(summary.getCalculationNodeId());
            boolean changed;
            do {
                changed=false;
                for(Map<String,Object> row:snapshot) {
                    if(row.get("parent_id") instanceof Number parent && descendants.contains(parent.longValue())) changed|=descendants.add(((Number)row.get("id")).longValue());
                }
            } while(changed);
            // A stored subtotal cannot be safely exposed when any constituent lies outside the scope.
            for(Map<String,Object> row:snapshot) {
                Long id=((Number)row.get("id")).longValue();
                if(!descendants.contains(id)) continue;
                nodes.requireNode(((Number)row.get("node_id")).longValue(),false);
                for(Map<String,Object> point:jdbc.queryForList("select collection_point_type,collection_point_id from emission_collection_node_data where calculation_node_id=?",id)) {
                    if(point.get("collection_point_id") instanceof Number pointId) collections.requireCollectionPoint(((Number)point.get("collection_point_type")).intValue(),pointId.longValue(),false);
                }
            }
            return snapshot.stream().anyMatch(row->Objects.equals(((Number)row.get("id")).longValue(),summary.getCalculationNodeId()));
        } catch(ResponseStatusException ex) {
            if(ex.getStatusCode().value()!=403 && ex.getStatusCode().value()!=404) throw ex;
            return false;
        }
    }
}
