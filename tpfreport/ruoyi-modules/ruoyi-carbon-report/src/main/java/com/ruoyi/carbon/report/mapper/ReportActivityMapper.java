package com.ruoyi.carbon.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.carbon.report.domain.ReportActivityRow;

public interface ReportActivityMapper
{
    int deleteFuels(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int deleteElectricity(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int deleteHeat(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int insertFuel(ReportActivityRow row);

    int insertElectricity(ReportActivityRow row);

    int insertHeat(ReportActivityRow row);

    int updateOverride(ReportActivityRow row);

    List<ReportActivityRow> selectFuels(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    List<ReportActivityRow> selectElectricity(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    List<ReportActivityRow> selectHeat(@Param("reportId") Long reportId, @Param("deptId") Long deptId);

    int countWorkload(@Param("reportId") Long reportId, @Param("deptId") Long deptId);
}
