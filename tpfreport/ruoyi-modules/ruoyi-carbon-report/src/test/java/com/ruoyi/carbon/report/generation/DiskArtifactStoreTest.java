package com.ruoyi.carbon.report.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

class DiskArtifactStoreTest
{
    @TempDir
    Path temp;

    @Test
    void forgedDeptCannotOpen() throws Exception
    {
        DiskArtifactStore store = new DiskArtifactStore(temp);
        Path file = store.prepare(301L, "abc", "docx", "docx");
        Files.write(file, new byte[] { 'P', 'K' });
        ReportFile meta = new ReportFile();
        meta.setDeptId(301L);
        meta.setObjectKey(store.relativeKey(file));
        assertThrows(ServiceException.class, () -> store.open(meta, 302L));
        assertEquals(301L, store.requireDept(meta, 301L).getDeptId());
        ServiceException crossDept = assertThrows(ServiceException.class, () -> store.requireDept(meta, 302L));
        assertEquals(HttpStatus.NOT_FOUND, crossDept.getCode());
        assertThrows(ServiceException.class, () -> store.requireDept(null, 301L));
    }

    @Test
    void reopenAfterNewStoreInstanceStillReadsFile() throws Exception
    {
        DiskArtifactStore store = new DiskArtifactStore(temp);
        Path file = store.prepare(301L, "abc", "docx", "docx");
        Files.write(file, new byte[] { 'P', 'K', 3, 4 });
        ReportFile meta = new ReportFile();
        meta.setId(88L);
        meta.setDeptId(301L);
        meta.setObjectKey(store.relativeKey(file));

        DiskArtifactStore restarted = new DiskArtifactStore(temp);
        try (InputStream in = restarted.open(meta, 301L))
        {
            assertEquals('P', in.read());
            assertEquals('K', in.read());
        }
        ServiceException hidden = assertThrows(ServiceException.class, () -> restarted.open(meta, 302L));
        assertEquals(HttpStatus.NOT_FOUND, hidden.getCode());
    }

    @Test
    void rejectsPathEscape()
    {
        DiskArtifactStore store = new DiskArtifactStore(temp);
        ReportFile meta = new ReportFile();
        meta.setDeptId(301L);
        meta.setObjectKey("../secret.bin");
        assertThrows(ServiceException.class, () -> store.resolve(meta));
    }
}
