package zawr.currencymonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
@SpringBootApplication
public class CurrencyMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyMonitorApplication.class, args);
    }

}
