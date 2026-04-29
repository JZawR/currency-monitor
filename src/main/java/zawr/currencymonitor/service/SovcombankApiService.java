package zawr.currencymonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import zawr.currencymonitor.model.SovcombankCurrencyRate;
import zawr.currencymonitor.properties.AppProperties;

import java.util.List;

@Service
@Slf4j
public class SovcombankApiService {

    private final WebClient webClient;
    private final AppProperties appProperties;

    public SovcombankApiService(WebClient webClient, AppProperties appProperties) {
        this.webClient = webClient;
        this.appProperties = appProperties;
        log.info("SovcombankApiService initialized with WebClient");
    }

    public List<SovcombankCurrencyRate> fetchCurrencyRates() {
        try {
            var props = appProperties.getSovcombank();

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/service/currency-rate/index")
                            .queryParam("lang", "ru")
                            .queryParam("type", props.getType())
                            .queryParam("departments", props.getDepartments())
                            .build())
                    .retrieve()
                    .bodyToFlux(SovcombankCurrencyRate.class)
                    .collectList()
                    .block(); // blocking OK для scheduled task

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
