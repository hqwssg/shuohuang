package com.ruoyi.carbon.report.generation;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.carbon.report.mapper.ReportGenerationJobMapper;

@Component
public class ReportGenerationRecovery implements ApplicationRunner
{
    private final ReportGenerationJobMapper jobMapper;

    public ReportGenerationRecovery(ReportGenerationJobMapper jobMapper)
    {
        this.jobMapper = jobMapper;
    }

    @Override
    public void run(ApplicationArguments args)
    {
        try
        {
            jobMapper.failStaleProcessing();
        }
        catch (Exception ex)
        {
            // Tables may not exist yet; do not block ruoyi-admin startup.
        }
    }
}
