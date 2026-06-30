package com.snaplearn.config;

import com.snaplearn.security.ApiKeyAuthFilter;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * API Key 认证：仅对 /api/v1/coze/** 生效
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyFilter() {
        FilterRegistrationBean<ApiKeyAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(apiKeyAuthFilter);
        reg.addUrlPatterns("/api/v1/coze/*");
        reg.setOrder(1);
        return reg;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/wechat-login",
                        "/api/v1/auth/dev-login",
                        "/api/v1/ocr/recognize",
                        "/api/v1/ocr/recognize-ai",
                        "/api/v1/coze/**",
                        "/api/v1/admin/login",
                        "/api/health"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(CurrentUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return webRequest.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
            }
        });
    }
}
