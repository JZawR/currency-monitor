package zawr.currencymonitor.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alert_states")
public class AlertState {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;  // например: "USD:RUB:SELL:80.0"

    private String base;
    private String quot;
    private String rateType; // "BUY" или "SELL"
    private Double threshold;

    private boolean alertSent;           // было ли отправлено уведомление
    private LocalDateTime lastAlertAt;   // когда отправили
    private LocalDateTime lastCheckedAt; // последний раз проверяли
    private Double lastCheckedRate;      // какое было значение

    // Уникальный ключ для поиска
    public static String makeKey(String base, String quot, String rateType, Double threshold) {
        return String.format("%s:%s:%s:%.2f",
                base.toUpperCase(), quot.toUpperCase(), rateType.toUpperCase(), threshold);
    }
}