package com.example.aptitude.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI aptitudeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Profession Aptitude API")
                        .version("v1")
                        .description("Session-based REST API that uses ChatGPT prompting to suggest professional aptitude."));
    }
}
