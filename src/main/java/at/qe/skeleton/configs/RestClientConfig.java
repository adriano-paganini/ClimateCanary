package at.qe.skeleton.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientConfig {

    @Bean
    public org.springframework.web.client.RestClient restClient() {
        return org.springframework.web.client.RestClient.builder().build();
    }
}
