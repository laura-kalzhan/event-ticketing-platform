package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;

public interface FeeRule {

    BigDecimal feeFor(BigDecimal subtotal, int ticketCount);
}
