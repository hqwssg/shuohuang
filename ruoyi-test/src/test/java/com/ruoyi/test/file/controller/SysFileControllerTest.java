package com.ruoyi.test.file.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.file.controller.SysFileController;
import com.ruoyi.file.service.ISysFileService;
import com.ruoyi.system.api.domain.SysFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class SysFileControllerTest
{
    private ISysFileService sysFileService;

    private SysFileController controller;

    @BeforeEach
    void setUp()
    {
        sysFileService = mock(ISysFileService.class);
        controller = new SysFileController();
        ReflectionTestUtils.setField(controller, "sysFileService", sysFileService);
    }

    @Test
    void uploadReturnsFileNameAndUrlFromStorageService()
            throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello".getBytes());
        when(sysFileService.uploadFile(file)).thenReturn("http://localhost/profile/demo.txt");

        R<SysFile> result = controller.upload(file);

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        assertThat(result.getData().getName()).isEqualTo("demo.txt");
        assertThat(result.getData().getUrl()).isEqualTo("http://localhost/profile/demo.txt");
        verify(sysFileService).uploadFile(file);
    }

    @Test
    void uploadReturnsFailureWhenStorageServiceThrows()
            throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello".getBytes());
        when(sysFileService.uploadFile(file)).thenThrow(new IllegalStateException("storage unavailable"));

        R<SysFile> result = controller.upload(file);

        assertThat(result.getCode()).isEqualTo(R.FAIL);
        assertThat(result.getMsg()).isEqualTo("storage unavailable");
    }

    @Test
    void deleteRejectsUnsafePathBeforeCallingStorageService()
    {
        R<Boolean> result = controller.delete("../application.yml");

        assertThat(result.getCode()).isEqualTo(R.FAIL);
        verifyNoInteractions(sysFileService);
    }

    @Test
    void deleteDelegatesValidFileUrlToStorageService()
            throws Exception
    {
        R<Boolean> result = controller.delete("http://localhost/profile/demo.txt");

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        verify(sysFileService).deleteFile("http://localhost/profile/demo.txt");
    }
}
