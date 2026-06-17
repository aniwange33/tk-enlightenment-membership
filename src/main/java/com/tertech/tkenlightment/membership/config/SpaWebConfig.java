package com.tertech.tkenlightment.membership.config;

import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the bundled React SPA (built into {@code classpath:/static/}) and falls back to
 * {@code index.html} for client-side routes (e.g. {@code /member}, {@code /admin}) so deep links
 * and refreshes work. Real files are served as-is; {@code /api/**} is left to the controllers.
 */
@org.springframework.context.annotation.Configuration
class SpaWebConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("/static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        if (!resourcePath.isEmpty()) {
                            Resource requested = location.createRelative(resourcePath);
                            if (requested.exists() && requested.isReadable()) {
                                return requested;
                            }
                        }
                        // Let the API 404 through its own handling; otherwise serve the SPA shell
                        // (covers "/" and client-side routes like /member, /admin).
                        return resourcePath.startsWith("api/") ? null : INDEX;
                    }
                });
    }
}
