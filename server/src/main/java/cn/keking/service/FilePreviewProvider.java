package cn.keking.service;

import cn.keking.model.FileAttribute;
import org.springframework.core.Ordered;

/**
 * Optional extension point for alternate preview handlers. An extension does
 * not replace a built-in Spring bean and is considered only when it supports
 * the provided FileAttribute. More specific handlers use a lower order value.
 */
public interface FilePreviewProvider extends FilePreview, Ordered {
    boolean supports(FileAttribute fileAttribute);

    @Override
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
