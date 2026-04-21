package zawr.currencymonitor.service;

import zawr.currencymonitor.model.CurrencyRate;
import zawr.currencymonitor.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SovcombankApiService {

    private final WebClient webClient;
    private final AppProperties appProperties;

    public List<CurrencyRate> fetchCurrencyRates() {
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
                    .bodyToFlux(CurrencyRate.class)
                    .collectList()
                    .block(); // blocking OK для scheduled task

        } catch (Exception e) {
            log.error("Failed to fetch currency rates", e);
            return List.of();
        }
    }
}
