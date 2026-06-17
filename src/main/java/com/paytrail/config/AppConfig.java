package com.paytrail.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytrail.util.EventParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public EventParser eventParser(ObjectMapper objectMapper) {
        return new EventParser(objectMapper);
    }
}
