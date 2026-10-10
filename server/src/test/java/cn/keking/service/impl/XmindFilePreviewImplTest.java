package cn.keking.service.impl;

import cn.keking.model.FileAttribute;
import cn.keking.service.FilePreview;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmindFilePreviewImplTest {

    @Test
    void showsXmindOnlyAfterSourceIsReady() {
        CommonPreviewImpl commonPreview = mock(CommonPreviewImpl.class);
        FileAttribute attr = mock(FileAttribute.class);
        ExtendedModelMap model = new ExtendedModelMap();
        when(commonPreview.filePreviewHandle(eq("https://files.example.com/a.xmind"), any(), eq(attr)))
                .thenReturn(null);
        assertEquals(FilePreview.XMIND_FILE_PREVIEW_PAGE,
                new XmindFilePreviewImpl(commonPreview)
                        .filePreviewHandle("https://files.example.com/a.xmind", model, attr));
    }

    @Test
    void propagatesDownloadFailureInsteadOfShowingEmptyMindMap() {
        CommonPreviewImpl commonPreview = mock(CommonPreviewImpl.class);
        FileAttribute attr = mock(FileAttribute.class);
        when(commonPreview.filePreviewHandle(eq("https://files.example.com/missing.xmind"), any(), eq(attr)))
                .thenReturn("notSupportedFile");
        assertEquals("notSupportedFile",
                new XmindFilePreviewImpl(commonPreview)
                        .filePreviewHandle("https://files.example.com/missing.xmind", new ExtendedModelMap(), attr));
    }
}
