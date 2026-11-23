package io.github.jonasfortes12.api.access;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.jonasfortes12.api.access.web.TermsCatalog;

@Configuration
@EnableConfigurationProperties(AccessProperties.class)
public class AccessConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Fails startup when the configured terms version has no file. */
    @Bean
    TermsCatalog termsCatalog(AccessProperties properties) {
        return TermsCatalog.load(properties.termsVersion());
    }
}
