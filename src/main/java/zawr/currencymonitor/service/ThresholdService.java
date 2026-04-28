package zawr.currencymonitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zawr.currencymonitor.entity.UserThreshold;
import zawr.currencymonitor.repository.UserThresholdRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThresholdService {

    private final UserThresholdRepository thresholdRepository;

    @Transactional
    public void setThreshold(String chatId, BigDecimal threshold) {
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }

        thresholdRepository.findByTelegramChatId(chatId)
                .map(existing -> {
                    existing.setUsdThreshold(threshold);
                    existing.setEnabled(true);
                    return thresholdRepository.save(existing);
                })
                .orElseGet(() -> {
                    var newThreshold = UserThreshold.builder()
                            .telegramChatId(chatId)
                            .usdThreshold(threshold)
                            .enabled(true)
                            .build();
                    return thresholdRepository.save(newThreshold);
                });
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getThreshold(String chatId) {
        return thresholdRepository.findByTelegramChatId(chatId)
                .filter(UserThreshold::getEnabled)
                .map(UserThreshold::getUsdThreshold);
    }

    @Transactional
    public void disableNotifications(String chatId) {
        thresholdRepository.findByTelegramChatId(chatId)
                .ifPresent(threshold -> {
                    threshold.setEnabled(false);
                    thresholdRepository.save(threshold);
                });
    }

    @Transactional(readOnly = true)
    public boolean shouldNotify(String chatId, BigDecimal currentRate) {
        return getThreshold(chatId)
                .map(threshold -> currentRate.compareTo(threshold) >= 0)
                .orElse(false);
    }
}