package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import java.math.BigDecimal;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;

public record PriceResponse(Long sectionId, String sectionName, BigDecimal price, String currency)
        implements Serializable {

    public static PriceResponse from(EventSectionPrice price) {
        return new PriceResponse(price.getSection().getId(), price.getSection().getName(), price.getPrice(),
                price.getCurrency());
    }
}
