package zawr.currencymonitor.repository;

import zawr.currencymonitor.model.CurrencyRate;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends MongoRepository<CurrencyRate, String> {

    Optional<CurrencyRate> findFirstByBaseAndQuotOrderByCreatedAtDateDateDesc(String base, String quot);

    List<CurrencyRate> findByBaseAndQuotAndCreatedAtDateDateAfter(
            String base, String quot, LocalDateTime after);

    boolean existsById(String id);

    // === НОВЫЕ МЕТОДЫ ДЛЯ ГРАФИКОВ ===

    // История за период (для детального графика)
    @Query("{ 'base': ?0, 'quot': ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } }")
    List<CurrencyRate> findHistoryByPeriod(
            String base, String quot, LocalDateTime from, LocalDateTime to);

    // Агрегация: средние значения по дням (для сводного графика)
    @Aggregation(pipeline = {
            "{ $match: { base: ?0, quot: ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } } }",
            "{ $group: { ",
            "  _id: { $dateTrunc: { date: { $toDate: '$createdAt.date.date' }, unit: 'day' } },",
            "  avgBuy: { $avg: { $toDouble: '$buy' } },",
            "  avgSell: { $avg: { $toDouble: '$sell' } },",
            "  minSell: { $min: { $toDouble: '$sell' } },",
            "  maxSell: { $max: { $toDouble: '$sell' } },",
            "  count: { $sum: 1 }",
            "}}",
            "{ $sort: { _id: 1 } }",
            "{ $project: { ",
            "  _id: 0, ",
            "  date: '$_id', ",
            "  avgBuy: 1, avgSell: 1, minSell: 1, maxSell: 1, count: 1 ",
            "}}"
    })
    List<DailyRateStats> findDailyStats(String base, String quot, LocalDateTime from, LocalDateTime to);

    // Агрегация: по часам (для детального графика за последние 24-48ч)
    @Aggregation(pipeline = {
            "{ $match: { base: ?0, quot: ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } } }",
            "{ $group: { ",
            "  _id: { $dateTrunc: { date: { $toDate: '$createdAt.date.date' }, unit: 'hour' } },",
            "  lastSell: { $last: { $toDouble: '$sell' } },",
            "  lastBuy: { $last: { $toDouble: '$buy' } }",
            "}}",
            "{ $sort: { _id: 1 } }",
            "{ $project: { _id: 0, date: '$_id', sell: '$lastSell', buy: '$lastBuy' }}"
    })
    List<HourlyRatePoint> findHourlyHistory(String base, String quot, LocalDateTime from, LocalDateTime to);

    // === DTO для агрегаций ===

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