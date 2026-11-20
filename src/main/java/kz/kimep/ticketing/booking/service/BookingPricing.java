package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;
import java.util.List;
import kz.kimep.ticketing.catalog.service.PricedSeat;
import kz.kimep.ticketing.shared.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingPricing {

    private final List<FeeRule> feeRules;

    public PriceTotals calculate(List<PricedSeat> seats) {
        if (seats.isEmpty()) {
            throw new BadRequestException("A booking needs at least one seat.");
        }
        String currency = seats.get(0).currency();
        if (seats.stream().anyMatch(seat -> !seat.currency().equals(currency))) {
            throw new BadRequestException("All seats in one booking must be priced in the same currency.");
        }
        BigDecimal subtotal = seats.stream().map(PricedSeat::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fees = feeRules.stream()
                .map(rule -> rule.feeFor(subtotal, seats.size()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PriceTotals(subtotal, fees, subtotal.add(fees), currency);
    }
}
