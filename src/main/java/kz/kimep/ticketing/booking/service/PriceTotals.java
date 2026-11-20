package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;

public record PriceTotals(BigDecimal subtotal, BigDecimal fees, BigDecimal total, String currency) {
}
