package zawr.currencymonitor.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import zawr.currencymonitor.model.CurrencyRate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CurrencyRateRepository extends MongoRepository<CurrencyRate, String> {

    // Правильный метод для поиска по дате (с учетом вложенной структуры)
    @Query("{ 'base': ?0, 'quot': ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } }")
    List<CurrencyRate> findHistoryByPeriod(String base, String quot, String from, String to);

    // Альтернативный метод с LocalDateTime
    default List<CurrencyRate> findHistoryByPeriod(String base, String quot, LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        String toStr = to.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        return findHistoryByPeriod(base, quot, fromStr, toStr);
    }

    // Получить последний курс
    @Query(value = "{ 'base': ?0, 'quot': ?1 }", sort = "{ 'createdAt.date.date': -1 }")
    Optional<CurrencyRate> findFirstByBaseAndQuotOrderByCreatedAtDateDateDesc(String base, String quot);

    // Вернуть оригинальные агрегационные методы
    @Aggregation(pipeline = {
            "{ $match: { base: ?0, quot: ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } } }",
            "{ $addFields: { " +
                    "  convertedDate: { " +
                    "    $dateFromString: { " +
                    "      dateString: { $substr: ['$createdAt.date.date', 0, 19] }, " +
                    "      format: '%Y-%m-%d %H:%M:%S' " +
                    "    } " +
                    "  } " +
                    "} }",
            "{ $group: { " +
                    "  _id: { $dateTrunc: { date: '$convertedDate', unit: 'day' } }, " +
                    "  avgBuy: { $avg: { $toDouble: '$buy' } }, " +
                    "  avgSell: { $avg: { $toDouble: '$sell' } }, " +
                    "  minSell: { $min: { $toDouble: '$sell' } }, " +
                    "  maxSell: { $max: { $toDouble: '$sell' } }, " +
                    "  count: { $sum: 1 } " +
                    "} }",
            "{ $sort: { _id: 1 } }",
            "{ $project: { " +
                    "  _id: 0, " +
                    "  date: '$_id', " +
                    "  avgBuy: 1, " +
                    "  avgSell: 1, " +
                    "  minSell: 1, " +
                    "  maxSell: 1, " +
                    "  count: 1 " +
                    "} }"
    })
    List<DailyRateStats> findDailyStats(String base, String quot, String from, String to);

    @Aggregation(pipeline = {
            "{ $match: { base: ?0, quot: ?1, 'createdAt.date.date': { $gte: ?2, $lte: ?3 } } }",
            "{ $addFields: { " +
                    "  convertedDate: { " +
                    "    $dateFromString: { " +
                    "      dateString: { $substr: ['$createdAt.date.date', 0, 19] }, " +
                    "      format: '%Y-%m-%d %H:%M:%S' " +
                    "    } " +
                    "  } " +
                    "} }",
            "{ $sort: { 'convertedDate': 1 } }",
            "{ $group: { " +
                    "  _id: { $dateTrunc: { date: '$convertedDate', unit: 'hour' } }, " +
                    "  lastSell: { $last: { $toDouble: '$sell' } }, " +
                    "  lastBuy: { $last: { $toDouble: '$buy' } } " +
                    "} }",
            "{ $sort: { _id: 1 } }",
            "{ $project: { _id: 0, date: '$_id', sell: '$lastSell', buy: '$lastBuy' } }"
    })
    List<HourlyRatePoint> findHourlyHistory(String base, String quot, String from, String to);

    // Интерфейсы для результатов агрегации
    interface DailyRateStats {
        LocalDateTime getDate();
        Double getAvgBuy();
        Double getAvgSell();
        Double getMinSell();
        Double getMaxSell();
        Integer getCount();
    }

    interface HourlyRatePoint {
        LocalDateTime getDate();
        Double getSell();
        Double getBuy();
    }
}