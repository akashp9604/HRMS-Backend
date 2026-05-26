package com.configserver.hrm.payrollService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // ✅ Add Basic Authentication interceptor
        // Replace with the same username/password you use in Postman or security config
        restTemplate.getInterceptors().add(
                new BasicAuthenticationInterceptor("ruchissonawane30@gmail.com", "Admin@123")
        );

        return restTemplate;
    }
}
