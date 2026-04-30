package zawr.currencymonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zawr.currencymonitor.entity.CurrencyRateEntity;

import java.util.List;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRateEntity, String> {
    @Query(value = """
        SELECT DISTINCT ON (bank) * 
        FROM currency_rate_entity
        ORDER BY bank, created_at DESC
        """, nativeQuery = true)
    List<CurrencyRateEntity> findLatestByEachBank();
}