package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ServiceFeeRule implements FeeRule {

    private static final BigDecimal RATE = new BigDecimal("0.05");

    @Override
    public BigDecimal feeFor(BigDecimal subtotal, int ticketCount) {
        return subtotal.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
