package zawr.currencymonitor.model;


import zawr.currencymonitor.dto.GraphQLResponse;
import zawr.currencymonitor.entity.CurrencyRateEntity;

import java.time.LocalDateTime;

public class CurrencyRateMapper {
    public static final String BBR = "BBR";
    public static final String SOVCOMBANK = "SOVCOMBANK";

    public static CurrencyRateEntity SovcombankModelToEntity(SovcombankCurrencyRate model) {
        CurrencyRateEntity rateEntity = new CurrencyRateEntity();
        rateEntity.setSell(model.getSell());
        rateEntity.setBase(model.getBase());
        rateEntity.setFetchedAt(model.getFetchedAt());
        rateEntity.setBank(SOVCOMBANK);
        return rateEntity;
    }

    public static CurrencyRateEntity BbrModelToEntity(GraphQLResponse.RateElement model) {
        CurrencyRateEntity rateEntity = new CurrencyRateEntity();
        rateEntity.setSell(String.valueOf(model.sellRate()));
        rateEntity.setBase(model.fromCurrency().code());
        rateEntity.setFetchedAt(LocalDateTime.now());
        rateEntity.setBank(BBR);
        return rateEntity;
    }
}
