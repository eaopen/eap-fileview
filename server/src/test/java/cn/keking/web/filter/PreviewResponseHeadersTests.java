package cn.keking.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class PreviewResponseHeadersTests {
    @Test void signedUrlPreviewDoesNotLeakThroughRefererOrCache() throws Exception {
        BaseUrlFilter filter = new BaseUrlFilter();
        for (String route : new String[]{"/onlinePreview", "/picturesPreview",
                "/getCorsFile", "/addTask"}) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "GET", "/fileview" + route);
            request.setContextPath("/fileview");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
        }
    }

    @Test void ordinaryResourcesKeepTheirExistingCacheBehavior() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/fileview/static/style.css");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new BaseUrlFilter().doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Cache-Control"));
    }
}
