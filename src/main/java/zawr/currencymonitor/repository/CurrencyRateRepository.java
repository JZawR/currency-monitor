package zawr.currencymonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zawr.currencymonitor.entity.CurrencyRateEntity;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRateEntity, String> {
}