package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;

public record PayableBooking(String reference, Long bookingId, BigDecimal total, String currency, long version) {
}
