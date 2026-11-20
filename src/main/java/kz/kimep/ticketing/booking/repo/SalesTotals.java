package kz.kimep.ticketing.booking.repo;

import java.math.BigDecimal;

public interface SalesTotals {

    Long getBookings();

    BigDecimal getRevenue();
}
