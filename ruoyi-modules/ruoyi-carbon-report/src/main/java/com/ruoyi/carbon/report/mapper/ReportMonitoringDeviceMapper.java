package com.ruoyi.carbon.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.carbon.report.domain.ReportMonitoringDevice;

public interface ReportMonitoringDeviceMapper
{
    List<ReportMonitoringDevice> selectByReport(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int deleteByReport(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int insert(ReportMonitoringDevice row);
}
