package cn.keking.service;

import cn.keking.model.FileAttribute;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Created by kl on 2018/1/17.
 * Content :
 */
@Service
public class FilePreviewFactory {

    private final ApplicationContext context;
    private volatile List<FilePreviewProvider> providers = List.of();

    /**
     * Optional injection: standalone service has no extra providers by default.
     * Extensions register distinct beans instead of overriding official handlers.
     */
    @Autowired(required = false)
    public void registerExtensions(List<FilePreviewProvider> extensions) {
        List<FilePreviewProvider> sorted = new ArrayList<>(extensions);
        AnnotationAwareOrderComparator.sort(sorted);
        providers = List.copyOf(sorted);
    }


    public FilePreviewFactory(ApplicationContext context) {
        this.context = context;
    }

    public FilePreview get(FileAttribute fileAttribute) {
        for (FilePreviewProvider provider : providers) {
            if (provider.supports(fileAttribute)) return provider;
        }
        return context.getBean(fileAttribute.getType().getInstanceName(), FilePreview.class);
    }
}
