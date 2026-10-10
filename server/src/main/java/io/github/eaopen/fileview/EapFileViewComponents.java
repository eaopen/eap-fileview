package io.github.eaopen.fileview;

import cn.keking.ServerMain;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Reusable preview engine component registration for downstream Boot apps.
 * Downstream applications own their entry point, while the upstream
 * executable entry point is explicitly excluded from embedded mode.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = "cn.keking",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = ServerMain.class
    )
)
public class EapFileViewComponents {
}
