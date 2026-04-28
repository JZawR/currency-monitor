package zawr.currencymonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zawr.currencymonitor.model.CurrencyRateEntity;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRateEntity, String> {
}