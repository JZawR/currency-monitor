package zawr.currencymonitor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import zawr.currencymonitor.model.CurrencyRate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends MongoRepository<CurrencyRate, String> {

    Optional<CurrencyRate> findFirstByBaseAndQuotOrderByCreatedAtDateDateDesc(String base, String quot);

    boolean existsById(String id);

    @Query("{ 'base': ?0, 'quot': ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } }")
    List<CurrencyRate> findHistoryByPeriod(
            String base, String quot, LocalDateTime from, LocalDateTime to);

    @Query("{ 'base': ?0, 'quot': ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } }")
    List<CurrencyRate> findByBaseAndQuotAndCreatedAtDateDateBetween(
            String base, String quot, String from, String to);

    interface DailyRateStats {
        java.time.LocalDateTime getDate();

        Double getAvgBuy();

        Double getAvgSell();

        Double getMinSell();

        Double getMaxSell();

        Integer getCount();
    }

    interface HourlyRatePoint {
        java.time.LocalDateTime getDate();

        Double getSell();

        Double getBuy();
    }
}