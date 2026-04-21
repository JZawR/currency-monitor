package zawr.currencymonitor.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zawr.currencymonitor.model.CurrencyRate;
import zawr.currencymonitor.repository.CurrencyRateRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HistoryService {

    private final CurrencyRateRepository repository;

    public HistoryService(CurrencyRateRepository repository) {
        this.repository = repository;
    }

    public List<CurrencyRateRepository.HourlyRatePoint> getHourlyHistory(String base, String quot,
                                                                         LocalDateTime from, LocalDateTime to) {
        // Форматируем даты для запроса
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<CurrencyRate> rates = repository.findByBaseAndQuotAndCreatedAtDateDateBetween(
                base, quot, fromStr, toStr);

        // Группировка по часам
        Map<LocalDateTime, HourlyRatePointImpl> hourlyMap = new LinkedHashMap<>();

        for (CurrencyRate rate : rates) {
            LocalDateTime rateTime = rate.getCreatedAt().getDate().toLocalDateTime();
            if (rateTime == null) continue;

            LocalDateTime hour = rateTime.truncatedTo(ChronoUnit.HOURS);

            HourlyRatePointImpl point = hourlyMap.computeIfAbsent(hour,
                    k -> new HourlyRatePointImpl(k, null, null));

            // Берем последнее значение за час (поскольку данные отсортированы по времени)
            point.setSell(rate.getSellAsDouble());
            point.setBuy(parseDouble(rate.getBuy()));
        }

        return new ArrayList<>(hourlyMap.values());
    }

    public List<CurrencyRateRepository.DailyRateStats> getDailyStats(String base, String quot,
                                                                     LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<CurrencyRate> rates = repository.findByBaseAndQuotAndCreatedAtDateDateBetween(
                base, quot, fromStr, toStr);

        // Группировка по дням
        Map<LocalDateTime, DailyRateStatsImpl> dailyMap = new LinkedHashMap<>();

        for (CurrencyRate rate : rates) {
            LocalDateTime rateTime = rate.getCreatedAt().getDate().toLocalDateTime();
            if (rateTime == null) continue;

            LocalDateTime day = rateTime.truncatedTo(ChronoUnit.DAYS);

            DailyRateStatsImpl stats = dailyMap.computeIfAbsent(day,
                    DailyRateStatsImpl::new);

            Double buy = parseDouble(rate.getBuy());
            Double sell = rate.getSellAsDouble();

            if (buy != null) stats.addBuy(buy);
            if (sell != null) stats.addSell(sell);
        }

        return new ArrayList<>(dailyMap.values());
    }

    private Double parseDouble(String value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Вспомогательные классы
    @Data
    @AllArgsConstructor
    private static class HourlyRatePointImpl implements CurrencyRateRepository.HourlyRatePoint {
        private LocalDateTime date;
        private Double sell;
        private Double buy;
    }

    @Data
    private static class DailyRateStatsImpl implements CurrencyRateRepository.DailyRateStats {
        private final LocalDateTime date;
        private double sumBuy = 0;
        private double sumSell = 0;
        private double minSell = Double.MAX_VALUE;
        private double maxSell = Double.MIN_VALUE;
        private int buyCount = 0;
        private int sellCount = 0;

        public void addBuy(Double buy) {
            if (buy != null) {
                sumBuy += buy;
                buyCount++;
            }
        }

        public void addSell(Double sell) {
            if (sell != null) {
                sumSell += sell;
                minSell = Math.min(minSell, sell);
                maxSell = Math.max(maxSell, sell);
                sellCount++;
            }
        }

        @Override
        public Double getAvgBuy() {
            return buyCount > 0 ? sumBuy / buyCount : null;
        }

        @Override
        public Double getAvgSell() {
            return sellCount > 0 ? sumSell / sellCount : null;
        }

        @Override
        public Double getMinSell() {
            return minSell == Double.MAX_VALUE ? null : minSell;
        }

        @Override
        public Double getMaxSell() {
            return maxSell == Double.MIN_VALUE ? null : maxSell;
        }

        @Override
        public Integer getCount() {
            return Math.max(buyCount, sellCount);
        }
    }
}