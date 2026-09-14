package com.ruoyi.carbon.report.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.carbon.report.domain.ReportGenerationJob;

public interface ReportGenerationJobMapper
{
    ReportGenerationJob selectById(Long id);

    String selectLatestSucceededSnapshot(@Param("deptId") Long deptId, @Param("reportYear") Integer reportYear);

    int insert(ReportGenerationJob job);

    int update(ReportGenerationJob job);

    int failStaleProcessing();

    ReportGenerationJob selectLatestSucceeded(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    Long selectLatestDocxFileId(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    Long selectLatestPdfFileId(@Param("reportId") Long reportId, @Param("deptId") Long deptId);
}
