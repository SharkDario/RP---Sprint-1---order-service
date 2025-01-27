package com.mindhub.order_service.util;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced // habilitate the charge balance
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // Specific error handling
                if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new RuntimeException("Resource not found");
                } else if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    throw new RuntimeException("Bad request");
                } else if (response.getStatusCode().is5xxServerError()) {
                    throw new RuntimeException("Server error");
                }
                // Others errors
                super.handleError(response);
            }
        });

        return restTemplate;
    }
}
