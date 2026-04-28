package zawr.currencymonitor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class CurrencyRateEntity {

    @Id
    private String id;
    private String base;
    private String sell;
    private LocalDateTime fetchedAt;
}
