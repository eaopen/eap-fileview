package cn.keking.service;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.Model;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilePreviewProviderTests {
    @Test
    void extensionCanHandleSpecificFilesWithoutReplacingBuiltInBean() {
        ApplicationContext context = mock(ApplicationContext.class);
        FilePreviewFactory factory = new FilePreviewFactory(context);
        FileAttribute attribute = new FileAttribute();
        attribute.setType(FileType.PDF);
        FilePreviewProvider extension = new FilePreviewProvider() {
            @Override public boolean supports(FileAttribute file) { return file.getType() == FileType.PDF; }
            @Override public String filePreviewHandle(String url, Model model, FileAttribute file) { return "custom-pdf"; }
        };
        factory.registerExtensions(List.of(extension));
        assertSame(extension, factory.get(attribute));
        verifyNoInteractions(context);
    }

    @Test
    void usesOriginalHandlerIfNoExtensionMatches() {
        ApplicationContext context = mock(ApplicationContext.class);
        FilePreview fallback = mock(FilePreview.class);
        FilePreviewFactory factory = new FilePreviewFactory(context);
        FileAttribute attribute = new FileAttribute();
        attribute.setType(FileType.PDF);
        when(context.getBean(FileType.PDF.getInstanceName(), FilePreview.class)).thenReturn(fallback);
        assertSame(fallback, factory.get(attribute));
    }

    @Test
    void explicitPriorityWins() {
        ApplicationContext context = mock(ApplicationContext.class);
        FilePreviewFactory factory = new FilePreviewFactory(context);
        FileAttribute file = new FileAttribute();
        file.setType(FileType.PDF);
        FilePreviewProvider low = provider(10);
        FilePreviewProvider high = provider(-10);
        factory.registerExtensions(List.of(low, high));
        assertSame(high, factory.get(file));
    }

    private static FilePreviewProvider provider(int priority) {
        return new FilePreviewProvider() {
            @Override public int getOrder() { return priority; }
            @Override public boolean supports(FileAttribute f) { return true; }
            @Override public String filePreviewHandle(String url, Model model, FileAttribute f) { return "pdf"; }
        };
    }
}
