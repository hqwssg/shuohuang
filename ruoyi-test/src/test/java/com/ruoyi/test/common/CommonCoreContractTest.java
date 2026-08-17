package com.ruoyi.test.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.UtilException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.sql.SqlUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommonCoreContractTest
{
    @Test
    void ajaxResultSuccessKeepsStatusAndAllowsChainablePayload()
    {
        AjaxResult result = AjaxResult.success().put("traceId", "trace-001");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result).containsEntry(AjaxResult.CODE_TAG, HttpStatus.SUCCESS)
                .containsEntry("traceId", "trace-001");
    }

    @Test
    void rFactoryMethodsExposeConsistentSuccessAndFailureContracts()
    {
        R<String> ok = R.ok("payload", "done");
        R<String> failed = R.fail("bad request");

        assertThat(R.isSuccess(ok)).isTrue();
        assertThat(ok.getData()).isEqualTo("payload");
        assertThat(ok.getMsg()).isEqualTo("done");
        assertThat(R.isError(failed)).isTrue();
        assertThat(failed.getCode()).isEqualTo(R.FAIL);
        assertThat(failed.getMsg()).isEqualTo("bad request");
    }

    @Test
    void stringUtilsCoversNamingConversionAndAntStyleMatching()
    {
        assertThat(StringUtils.toUnderScoreCase("sysUserName")).isEqualTo("sys_user_name");
        assertThat(StringUtils.toCamelCase("sys_user_name")).isEqualTo("sysUserName");
        assertThat(StringUtils.convertToCamelCase("sys_user_name")).isEqualTo("SysUserName");
        assertThat(StringUtils.matches("/system/user/list", List.of("/system/**", "/monitor/**"))).isTrue();
    }

    @Test
    void sqlUtilAcceptsSafeOrderByAndRejectsUnsafeInput()
    {
        assertThat(SqlUtil.escapeOrderBySql("create_time desc, user_name asc"))
                .isEqualTo("create_time desc, user_name asc");

        assertThatThrownBy(() -> SqlUtil.escapeOrderBySql("name desc; drop table sys_user"))
                .isInstanceOf(UtilException.class);
        assertThatThrownBy(() -> SqlUtil.filterKeyword("select * from sys_user"))
                .isInstanceOf(UtilException.class);
    }
}
