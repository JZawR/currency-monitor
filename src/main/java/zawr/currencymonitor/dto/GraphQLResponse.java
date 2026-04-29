package zawr.currencymonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLResponse(
        @JsonProperty("data") DataWrapper data,
        @JsonProperty("errors") List<GraphQLError> errors
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataWrapper(@JsonProperty("rates") RatesData rates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RatesData(
            @JsonProperty("actualAt") String actualAt,
            @JsonProperty("elements") List<RateElement> elements
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateElement(
            int id,
            String rateType,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal buyRate,
            String buyRateStatus,
            BigDecimal sellRate,
            String sellRateStatus,
            int lot
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Currency(String code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphQLError(String message, List<Object> path) {}
}