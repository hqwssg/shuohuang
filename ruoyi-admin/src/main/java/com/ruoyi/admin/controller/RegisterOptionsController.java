package com.ruoyi.admin.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only organization options used by self-registration. */
@RestController
@RequestMapping("/auth/register")
public class RegisterOptionsController
{
    private final JdbcTemplate jdbc;

    public RegisterOptionsController(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    @GetMapping("/departments")
    public AjaxResult departments()
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select dept_id as deptId,parent_id as parentId,dept_name as deptName "
                        + "from sys_dept where del_flag='0' and status='0' "
                        + "order by parent_id,order_num,dept_id");
        return AjaxResult.success(rows);
    }
}
