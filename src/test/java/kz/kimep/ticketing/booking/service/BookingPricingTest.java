package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import kz.kimep.ticketing.catalog.service.PricedSeat;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.Test;

class BookingPricingTest {

    private final BookingPricing pricing = new BookingPricing(List.of(new ServiceFeeRule()));

    @Test
    void addsAFivePercentServiceFee() {
        PriceTotals totals = pricing.calculate(List.of(TestData.seat(1L, "25000"), TestData.seat(2L, "12000")));

        assertThat(totals.subtotal()).isEqualByComparingTo("37000");
        assertThat(totals.fees()).isEqualByComparingTo("1850");
        assertThat(totals.total()).isEqualByComparingTo("38850");
    }

    @Test
    void everyFeeRuleIsAddedWithoutChangingThePricingClass() {
        FeeRule flatFee = (subtotal, count) -> new BigDecimal("100").multiply(BigDecimal.valueOf(count));
        BookingPricing withExtraRule = new BookingPricing(List.of(new ServiceFeeRule(), flatFee));

        PriceTotals totals = withExtraRule.calculate(List.of(TestData.seat(1L, "10000"), TestData.seat(2L, "10000")));

        assertThat(totals.fees()).isEqualByComparingTo("1200");
    }

    @Test
    void mixedCurrenciesAreRejected() {
        PricedSeat dollars = new PricedSeat(2L, 3L, "South", "A", 2, new BigDecimal("50"), "USD");

        assertThatThrownBy(() -> pricing.calculate(List.of(TestData.seat(1L, "12000"), dollars)))
                .isInstanceOf(BadRequestException.class);
    }
}
