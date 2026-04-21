package zawr.currencymonitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "currency_rates")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRate {

    @Id
    private String id;

    private Long messageId;
    private String type;
    private String base;
    private String quot;
    private String buy;
    private String sell;
    private String departmentId;

    private java.util.List<Interval> intervals;

    @JsonProperty("created_at")
    private TimestampInfo createdAt;

    @JsonProperty("begins_at")
    private TimestampInfo beginsAt;

    // Поле для отслеживания, было ли отправлено уведомление по этой записи
    @Builder.Default
    private boolean notificationSent = false;

    public Double getSellAsDouble() {
        try {
            return Double.parseDouble(sell);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Interval {
        private String buy;
        private String sell;
        private Integer from;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimestampInfo {
        private TimestampDate date;
        private Integer timezoneType;
        private String timezone;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TimestampDate {
            private String date; // "2026-04-21 01:37:53.000000"

            public LocalDateTime toLocalDateTime() {
                if (date == null) return null;
                // Убираем микросекунды для парсинга
                String cleaned = date.replace(".000000", "");
                return LocalDateTime.parse(cleaned,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        }
    }
}
