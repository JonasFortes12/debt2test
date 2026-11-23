package io.github.jonasfortes12.api.access.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.jonasfortes12.api.access.AccessService;

/** Registers the access-code check on every pipeline endpoint. The access endpoints are not guarded by it. */
@Configuration
public class AccessWebConfig implements WebMvcConfigurer {

    private final AccessService accessService;

    public AccessWebConfig(AccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessCodeInterceptor(accessService)).addPathPatterns("/api/pipeline/**");
    }
}
