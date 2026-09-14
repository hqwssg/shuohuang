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

    public CarbonDataScopeService(TemplateRepository templateRepository, EmissionNodeRepository nodeRepository) {
        this.templateRepository = templateRepository;
        this.nodeRepository = nodeRepository;
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
    }

    public boolean canReadSafely(Template template) {
        try {
            return canRead(template);
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    public NodeDTO filterTree(NodeDTO root) {
        CarbonSecurityContext context = current();
        if (root == null || context.allData() || context.nodeIds().isEmpty()) return root;
        return retainAuthorizedNodes(root, false, context.nodeIds()) ? root : null;
    }

    private boolean retainAuthorizedNodes(NodeDTO node, boolean insideAssignedTree, java.util.List<Long> assignedIds) {
        boolean currentAssigned = insideAssignedTree || assignedIds.contains(node.getId());
        java.util.List<NodeDTO> children = node.getChildren() == null
                ? new java.util.ArrayList<>() : new java.util.ArrayList<>(node.getChildren());
        children.removeIf(child -> !retainAuthorizedNodes(child, currentAssigned, assignedIds));
        node.setChildren(children);
        if (!currentAssigned && !children.isEmpty()) {
            node.setConfig(null);
            node.setNodeInfo(null);
        }
        return currentAssigned || !children.isEmpty();
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
