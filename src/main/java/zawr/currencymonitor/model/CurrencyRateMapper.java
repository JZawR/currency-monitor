package zawr.currencymonitor.model;


public class CurrencyRateMapper {

    public static CurrencyRate entityToModel(CurrencyRateEntity rateEntity) {
        CurrencyRate model = new CurrencyRate();
        model.setId(rateEntity.getId());
        model.setSell(rateEntity.getSell());
        model.setBase(rateEntity.getBase());
        model.setFetchedAt(rateEntity.getFetchedAt());
        return model;
    }

    public static CurrencyRateEntity modelToEntity(CurrencyRate model) {
        CurrencyRateEntity rateEntity = new CurrencyRateEntity();
        rateEntity.setId(model.getId());
        rateEntity.setSell(model.getSell());
        rateEntity.setBase(model.getBase());
        rateEntity.setFetchedAt(model.getFetchedAt());
        return rateEntity;
    }
}
