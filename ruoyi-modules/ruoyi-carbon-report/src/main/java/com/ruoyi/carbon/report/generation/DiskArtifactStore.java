package com.ruoyi.carbon.report.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

public class DiskArtifactStore
{
    public static final class StoredFile
    {
        public final ReportFile meta;
        public final Path path;

        public StoredFile(ReportFile meta, Path path)
        {
            this.meta = meta;
            this.path = path;
        }
    }

    private final Path artifactRoot;

    public DiskArtifactStore(Path artifactRoot)
    {
        this.artifactRoot = artifactRoot.toAbsolutePath().normalize();
    }

    public Path resolve(ReportFile meta)
    {
        Path path = artifactRoot.resolve(meta.getObjectKey()).normalize();
        if (!path.startsWith(artifactRoot))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
        return path;
    }

    public Path prepare(Long deptId, String snapshotSha, String role, String extension)
    {
        Path dir = artifactRoot.resolve(String.valueOf(deptId)).resolve(snapshotSha);
        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException ex)
        {
            throw new ServiceException("无法创建产物目录: " + ex.getMessage());
        }
        return dir.resolve(role + "." + extension);
    }

    public Path prepareUpload(Long deptId, Long reportId, String role, String extension)
    {
        Path dir = artifactRoot.resolve(String.valueOf(deptId)).resolve("uploads").resolve(String.valueOf(reportId));
        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException ex)
        {
            throw new ServiceException("无法创建上传目录: " + ex.getMessage());
        }
        return dir.resolve(role + "." + extension);
    }

    public String relativeKey(Path file)
    {
        return artifactRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    public void moveInto(Path source, Path target)
    {
        try
        {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException ex)
        {
            try
            {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException copyEx)
            {
                throw new ServiceException("无法保存渲染产物: " + copyEx.getMessage());
            }
        }
    }

    public InputStream open(ReportFile meta, Long deptId) throws IOException
    {
        if (meta == null || deptId == null || !deptId.equals(meta.getDeptId()))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
        Path path = resolve(meta);
        if (!Files.isRegularFile(path))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
        return Files.newInputStream(path);
    }

    public ReportFile requireDept(ReportFile meta, Long deptId)
    {
        if (meta == null || deptId == null || !deptId.equals(meta.getDeptId()))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
        return meta;
    }
}
