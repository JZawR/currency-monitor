package zawr.currencymonitor.model;


import zawr.currencymonitor.entity.CurrencyRateEntity;

public class CurrencyRateMapper {

    public static CurrencyRateEntity modelToEntity(CurrencyRate model) {
        CurrencyRateEntity rateEntity = new CurrencyRateEntity();
        rateEntity.setId(model.getId());
        rateEntity.setSell(model.getSell());
        rateEntity.setBase(model.getBase());
        rateEntity.setFetchedAt(model.getFetchedAt());
        return rateEntity;
    }
}
