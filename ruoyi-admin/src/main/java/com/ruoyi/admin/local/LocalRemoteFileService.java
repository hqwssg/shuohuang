package com.ruoyi.admin.local;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.file.service.ISysFileService;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalRemoteFileService implements RemoteFileService
{
    private static final Logger log = LoggerFactory.getLogger(LocalRemoteFileService.class);

    private final ISysFileService sysFileService;

    public LocalRemoteFileService(ISysFileService sysFileService)
    {
        this.sysFileService = sysFileService;
    }

    @Override
    public R<SysFile> upload(MultipartFile file)
    {
        try
        {
            String url = sysFileService.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);
        }
        catch (Exception e)
        {
            log.error("Upload file failed", e);
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Boolean> delete(String fileUrl)
    {
        try
        {
            if (!FileUtils.validateFilePath(fileUrl))
            {
                throw new Exception(StringUtils.format("Resource file ({}) is invalid and cannot be deleted.", fileUrl));
            }
            sysFileService.deleteFile(fileUrl);
            return R.ok(true);
        }
        catch (Exception e)
        {
            log.error("Delete file failed", e);
            return R.fail(e.getMessage());
        }
    }
}
