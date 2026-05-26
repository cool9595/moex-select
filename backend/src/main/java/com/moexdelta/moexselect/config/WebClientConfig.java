package com.moexdelta.moexselect.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient moexIssWebClient(WebClient.Builder builder) {
        var httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(8));
        return builder
            .baseUrl("https://iss.moex.com")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
