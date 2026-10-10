package cn.keking.service.impl;

import cn.keking.model.FileAttribute;
import cn.keking.service.FilePreview;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * svg 图片文件处理
 * @author kl (http://kailing.pub)
 * @since 2021/2/8
 */
@Service
public class XmindFilePreviewImpl implements FilePreview {

    private final CommonPreviewImpl commonPreview;

    public XmindFilePreviewImpl(CommonPreviewImpl commonPreview) {
        this.commonPreview = commonPreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 修改逻辑：通用取流入口失败时直接展示错误页，成功才进入 XMind 查看器。
        // 修改原因：复用旧版文件预览修复的通用思路，避免下载失败却显示空白导图。
        String fallbackPage = commonPreview.filePreviewHandle(url, model, fileAttribute);
        return fallbackPage == null ? XMIND_FILE_PREVIEW_PAGE : fallbackPage;
    }
}
