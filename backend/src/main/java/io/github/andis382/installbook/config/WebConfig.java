package io.github.andis382.installbook.config;

import java.io.IOException;
import java.time.Clock;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Serves the built SPA from classpath:/static and falls back to index.html for client-side
     * routes, so a reload on /units/42 still works in production.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                        return null;
                    }
                    Resource requested = location.createRelative(resourcePath);
                    if (requested.exists() && requested.isReadable()) {
                        return requested;
                    }
                    Resource index = new ClassPathResource("/static/index.html");
                    return index.exists() ? index : null;
                }
            });
    }

    /** Validation messages come from messages*.properties, so they follow the request language. */
    @Bean
    LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
