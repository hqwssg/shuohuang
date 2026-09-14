package cn.com.v2.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cn.com.v2.model.GoviewProject;
import cn.com.v2.service.IGoviewProjectService;

@Service
public class GoviewDataScopeService
{
    private final HttpServletRequest request;
    private final IGoviewProjectService projectService;

    public GoviewDataScopeService(HttpServletRequest request, IGoviewProjectService projectService)
    {
        this.request = request;
        this.projectService = projectService;
    }

    public RuoYiSecurityContext current()
    {
        Object value = request.getAttribute(RuoYiSecurityContext.REQUEST_ATTRIBUTE);
        if (value instanceof RuoYiSecurityContext) return (RuoYiSecurityContext) value;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing RuoYi security context");
    }

    public void requireProject(String projectId, boolean write)
    {
        GoviewProject project = projectService.getById(projectId);
        if (project == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GoView project not found");
        RuoYiSecurityContext context = current();
        if (context.isAllData()) return;
        if (project.getDeptId() == null)
        {
            if (!write) return;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Legacy global projects are read-only");
        }
        boolean allowed = context.isSelfOnly()
                ? String.valueOf(context.getUserId()).equals(project.getCreateUserId())
                : context.getDeptIds().contains(project.getDeptId());
        if (!allowed)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GoView project is outside the current data scope");
        }
    }
}
