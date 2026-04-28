package zawr.currencymonitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String telegramChatId;
    private String base;
    private String quot;
    private String rateType; // "BUY" или "SELL"
    private Double threshold;

    private boolean alertSent;           // было ли отправлено уведомление
    private LocalDateTime lastAlertAt;   // когда отправили
    private LocalDateTime lastCheckedAt; // последний раз проверяли
    private Double lastCheckedRate;      // какое было значение
}