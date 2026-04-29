package zawr.currencymonitor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class CurrencyRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String base;
    private String sell;
    private String bank;
    private LocalDateTime fetchedAt;
}
