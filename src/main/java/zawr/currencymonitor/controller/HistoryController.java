package zawr.currencymonitor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zawr.currencymonitor.model.AlertState;
import zawr.currencymonitor.model.CurrencyRate;
import zawr.currencymonitor.repository.AlertStateRepository;
import zawr.currencymonitor.repository.CurrencyRateRepository;
import zawr.currencymonitor.service.HistoryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final CurrencyRateRepository repository;
    private final AlertStateRepository alertRepository;
    private final HistoryService historyService;

    // 📊 Детальная история (все записи)
    @GetMapping("/raw")
    public ResponseEntity<List<CurrencyRate>> getRawHistory(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "RUB") String quot,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(
                repository.findHistoryByPeriod(base, quot, from, to)
        );
    }

    // 📈 Агрегация по дням (для сводного графика)
    @GetMapping("/daily")
    public ResponseEntity<List<CurrencyRateRepository.DailyRateStats>> getDailyStats(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "RUB") String quot,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().minusDays(30)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        // ИСПРАВЛЕНО: используем historyService вместо прямого вызова repository
        return ResponseEntity.ok(
                historyService.getDailyStats(base, quot, from, to)
        );
    }

    // ⏱ Агрегация по часам (для детального графика за последние 48ч)
    @GetMapping("/hourly")
    public ResponseEntity<List<CurrencyRateRepository.HourlyRatePoint>> getHourlyHistory(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "RUB") String quot,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().minusHours(48)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        // ИСПРАВЛЕНО: используем historyService вместо прямого вызова repository
        List<CurrencyRateRepository.HourlyRatePoint> result = historyService.getHourlyHistory(base, quot, from, to);
        log.info("getHourlyHistory: {} entries", result.size());
        return ResponseEntity.ok(result);
    }

    // 🎯 Последнее значение + статистика за 24ч
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "RUB") String quot) {

        var now = LocalDateTime.now();
        var yesterday = now.minusHours(24);

        var latest = repository.findFirstByBaseAndQuotOrderByCreatedAtDateDateDesc(base, quot);
        var history24h = repository.findHistoryByPeriod(base, quot, yesterday, now);

        var response = new java.util.HashMap<String, Object>();
        response.put("latest", latest.orElse(null));

        if (!history24h.isEmpty()) {
            var sells = history24h.stream()
                    .map(CurrencyRate::getSellAsDouble)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            response.put("stats24h", Map.of(
                    "min", sells.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN),
                    "max", sells.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN),
                    "avg", sells.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN),
                    "samples", sells.size()
            ));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alert-state")
    public ResponseEntity<Map<String, Object>> getAlertState(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "RUB") String quot,
            @RequestParam Double threshold) {

        String key = AlertState.makeKey(base, quot, "SELL", threshold);
        Optional<AlertState> stateOpt = alertRepository.findByKey(key);

        if (stateOpt.isPresent()) {
            AlertState state = stateOpt.get();
            return ResponseEntity.ok(Map.of(
                    "key", state.getKey(),
                    "alertSent", state.isAlertSent(),
                    "lastAlertAt", state.getLastAlertAt(),
                    "lastCheckedRate", state.getLastCheckedRate()
            ));
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("key", key);
            response.put("status", "not_initialized");
            return ResponseEntity.ok(response);
        }
    }
}