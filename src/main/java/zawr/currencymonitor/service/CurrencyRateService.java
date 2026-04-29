package zawr.currencymonitor.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zawr.currencymonitor.entity.CurrencyRateEntity;
import zawr.currencymonitor.repository.CurrencyRateRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CurrencyRateService {
    private final CurrencyRateRepository currencyRateRepository;

    public void save(CurrencyRateEntity rate) {
        currencyRateRepository.save(rate);
    }
}
