package zawr.currencymonitor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import zawr.currencymonitor.properties.AppProperties;

@RequiredArgsConstructor
@Configuration
public class WebClientConfig {
    private final AppProperties appProperties;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(appProperties.getSovcombank().getApiUrl())
                .build();
    }
}
