package com.ruoyi.carbon.report.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.carbon.report.domain.ReportFile;

public interface ReportFileMapper
{
    ReportFile selectById(Long id);

    ReportFile selectByDeptSnapshotRole(@Param("deptId") Long deptId,
            @Param("snapshotSha256") String snapshotSha256, @Param("role") String role);

    java.util.List<ReportFile> selectUploadsByReport(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int deleteUploadByReportRole(@Param("reportId") Long reportId, @Param("deptId") Long deptId, @Param("role") String role);

    int insert(ReportFile file);
}
