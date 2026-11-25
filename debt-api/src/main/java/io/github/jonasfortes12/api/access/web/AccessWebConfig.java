package io.github.jonasfortes12.api.access.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.jonasfortes12.api.access.AccessService;

/** Registers the access-code check on every pipeline endpoint. The access endpoints are not guarded by it. */
@Configuration
public class AccessWebConfig implements WebMvcConfigurer {

    private final AccessService accessService;
    private final StudyKey studyKey;

    public AccessWebConfig(AccessService accessService, @Value("${debt.study.api-key:}") String studyApiKey) {
        this.accessService = accessService;
        this.studyKey = new StudyKey(studyApiKey);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessCodeInterceptor(accessService, studyKey)).addPathPatterns("/api/pipeline/**");
    }
}
