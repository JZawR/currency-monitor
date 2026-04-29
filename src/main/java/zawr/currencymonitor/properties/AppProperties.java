package zawr.currencymonitor.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Sovcombank sovcombank = new Sovcombank();
    private Telegram telegram = new Telegram();
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Sovcombank {
        private String apiUrl = "https://prod-api.sovcombank.ru";
        private String departments = "7100360";
        private String type = "FIZ_NAL";
    }

    @Data
    public static class Telegram {
        private String botToken;
        private String botUsername;
        private Double thresholdUsdSell = 80.0;
        private Boolean antiSpamEnabled = true;
        private Long alertCooldownMinutes = 60L;
    }

    @Data
    public static class Scheduler {
        private String cron = "0 0/30 * * * *";
    }
}