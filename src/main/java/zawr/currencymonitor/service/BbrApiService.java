package zawr.currencymonitor.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import zawr.currencymonitor.dto.GraphQLResponse;
import zawr.currencymonitor.exception.BbrApiException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BbrApiService {

    private final WebClient webClient;

    public BbrApiService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://bbr.ru/graphql/")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<List<GraphQLResponse.RateElement>> fetchRates(String citySlug, int officeId, String rateType) {
        String query = """
                query RatesList($rateType: RateTypeEnum, $citySlug: String, $range: InputRateRange, $officeId: Int) {
                  rates(
                    noPagination: true
                    rateType: $rateType
                    citySlug: $citySlug
                    officeId: $officeId
                    range: $range
                  ) {
                    actualAt
                    elements {
                      id
                      rateType
                      fromCurrency { code }
                      toCurrency { code }
                      buyRate
                      buyRateStatus
                      sellRate
                      sellRateStatus
                      lot
                    }
                  }
                }
                """;

        // range опционален, передаём только то, что нужно
        Map<String, Object> variables = Map.of(
                "rateType", rateType,
                "citySlug", citySlug,
                "officeId", officeId
        );

        GraphQLRequest request = new GraphQLRequest(query, variables);

        return webClient.post()
                .uri("/")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GraphQLResponse.class)
                .handle((response, sink) -> {
                    // GraphQL всегда возвращает 200, даже при ошибках
                    if (response.errors() != null && !response.errors().isEmpty()) {
                        sink.error(new BbrApiException("GraphQL errors: " + response.errors()));
                        return;
                    }
                    if (response.data() == null || response.data().rates() == null) {
                        sink.next(Collections.emptyList());
                        return;
                    }
                    sink.next(response.data().rates().elements());
                });
    }

    // Простой DTO для тела запроса
    public record GraphQLRequest(String query, Map<String, Object> variables) {
    }
}