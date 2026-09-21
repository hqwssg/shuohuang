package com.example.carbon.emission.model.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.dto.NodeDTO;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.repository.TemplateRepository;

@Service
public class CarbonDataScopeService {
    private final TemplateRepository templateRepository;
    private final EmissionNodeRepository nodeRepository;
    private final CollectionScopeService collectionScopes;
    private final com.example.carbon.emission.model.repository.EmissionNodeConfigRepository configs;

    public CarbonDataScopeService(TemplateRepository templateRepository, EmissionNodeRepository nodeRepository,
            CollectionScopeService collectionScopes,
            com.example.carbon.emission.model.repository.EmissionNodeConfigRepository configs) {
        this.templateRepository = templateRepository;
        this.nodeRepository = nodeRepository;
        this.collectionScopes = collectionScopes;
        this.configs = configs;
    }

    public CarbonSecurityContext current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object value = attributes.getRequest().getAttribute(CarbonSecurityContext.REQUEST_ATTRIBUTE);
            if (value instanceof CarbonSecurityContext context) return context;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing carbon security context");
    }

    public boolean canRead(Template template) {
        CarbonSecurityContext context = current();
        if (context.allData() || template.getDeptId() == null) return true;
        if (context.selfOnly()) return context.userId().equals(template.getCreatedBy());
        return context.deptIds().contains(template.getDeptId());
    }

    public void requireTemplate(Long templateId, boolean write) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        boolean allowed = write ? canWrite(template) : canRead(template);
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Template is outside the current data scope");
    }

    public void requireNode(Long nodeId, boolean write) {
        EmissionNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        requireTemplate(node.getTemplateId(), write);
        if (!isInAssignedNodeTree(node)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Node is outside the assigned accounting scope");
        }
        configs.findByNodeId(nodeId).ifPresent(config -> {
            if (config.getCollectionPointId() != null) {
                collectionScopes.requireCollectionPoint(config.getCollectionPointType(), config.getCollectionPointId(), false);
            }
        });
    }

    public boolean canReadSafely(Template template) {
        try {
            return canRead(template);
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    public void requireNodeTree(Long nodeId, boolean write) {
        requireNodeTree(nodeId, write, new java.util.HashSet<>());
    }

    private void requireNodeTree(Long nodeId, boolean write, java.util.Set<Long> visited) {
        if (!visited.add(nodeId)) return;
        requireNode(nodeId, write);
        for (EmissionNode child : nodeRepository.findByParentId(nodeId)) requireNodeTree(child.getId(), write, visited);
    }

    public void requireTemplateTree(Long templateId) {
        requireTemplate(templateId, false);
        for (EmissionNode node : nodeRepository.findByTemplateId(templateId)) requireNode(node.getId(), false);
    }

    public NodeDTO filterTree(NodeDTO root) {
        CarbonSecurityContext context = current();
        if (root == null || context.allData()) return root;
        return retainAuthorizedNodes(root, false, context.nodeIds()) ? root : null;
    }

    private boolean retainAuthorizedNodes(NodeDTO node, boolean insideAssignedTree, java.util.List<Long> assignedIds) {
        boolean currentAssigned = assignedIds.isEmpty() || insideAssignedTree || assignedIds.contains(node.getId());
        boolean physicalAllowed = true;
        if (node.getConfig() != null && node.getConfig().getCollectionPointId() != null) {
            try {
                collectionScopes.requireCollectionPoint(node.getConfig().getCollectionPointType(), node.getConfig().getCollectionPointId(), false);
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() != 403 && ex.getStatusCode().value() != 404) throw ex;
                physicalAllowed = false;
            }
        }
        java.util.List<NodeDTO> children = node.getChildren() == null
                ? new java.util.ArrayList<>() : new java.util.ArrayList<>(node.getChildren());
        int originalCount = children.size();
        children.removeIf(child -> !retainAuthorizedNodes(child, currentAssigned, assignedIds));
        node.setChildren(children);
        if (!currentAssigned || !physicalAllowed || originalCount != children.size()) {
            node.setConfig(null);
            node.setNodeInfo(null);
        }
        if (originalCount > 0 && children.isEmpty() && (node.getConfig() == null || node.getConfig().getCollectionPointId() == null)) return false;
        return (currentAssigned && physicalAllowed) || !children.isEmpty();
    }

    private boolean canWrite(Template template) {
        CarbonSecurityContext context = current();
        if (context.allData()) return true;
        if (template.getDeptId() == null) return false;
        if (context.selfOnly()) return context.userId().equals(template.getCreatedBy());
        return context.deptIds().contains(template.getDeptId());
    }

    private boolean isInAssignedNodeTree(EmissionNode node) {
        CarbonSecurityContext context = current();
        if (context.allData() || context.nodeIds().isEmpty()) return true;
        EmissionNode current = node;
        while (current != null) {
            if (context.nodeIds().contains(current.getId())) return true;
            current = current.getParentId() == null ? null : nodeRepository.findById(current.getParentId()).orElse(null);
        }
        return false;
    }
}
