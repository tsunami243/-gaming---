package org.example.bigevent;

import org.example.bigevent.Interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns("/user/login", "/user/register",
                    "/article/carousel", "/article/stats", "/article/heat",
                    "/article-view/**",
                    "/comment/**", "/article-like/**", "/comment-like/**",
                    "/article-favorite/**");

    }
}
