package zawr.currencymonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zawr.currencymonitor.repository.CurrencyRateRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class HistoryService {

    private final CurrencyRateRepository repository;

    public HistoryService(CurrencyRateRepository repository) {
        this.repository = repository;
    }

    public List<CurrencyRateRepository.HourlyRatePoint> getHourlyHistory(String base, String quot,
                                                                         LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("Fetching hourly history for {}/{} from {} to {}", base, quot, fromStr, toStr);
        List<CurrencyRateRepository.HourlyRatePoint> result = repository.findHourlyHistory(base, quot, fromStr, toStr);
        log.info("Found {} hourly records", result.size());

        return result;
    }

    public List<CurrencyRateRepository.DailyRateStats> getDailyStats(String base, String quot,
                                                                     LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("Fetching daily stats for {}/{} from {} to {}", base, quot, fromStr, toStr);
        List<CurrencyRateRepository.DailyRateStats> result = repository.findDailyStats(base, quot, fromStr, toStr);
        log.info("Found {} daily records", result.size());

        return result;
    }

    // Этот метод больше не нужен, используем прямой вызов репозитория
    // public List<CurrencyRate> getRawHistory(...) - используйте repository.findHistoryByPeriod
}