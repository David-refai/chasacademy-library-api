package com.library.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// إعداد RestTemplate لإرسال طلبات HTTP لـ APIs خارجية
// يُستخدم في ExternalBookService للتواصل مع Open Library API
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
