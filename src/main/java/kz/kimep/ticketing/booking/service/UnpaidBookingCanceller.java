package kz.kimep.ticketing.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidBookingCanceller {

    private final BookingLifecycleService bookingLifecycleService;

    @Scheduled(fixedDelayString = "${ticketing.unpaid-check-interval:60000}")
    public void expireUnpaidBookings() {
        int expired = bookingLifecycleService.expireUnpaid();
        if (expired > 0) {
            log.info("Expired {} bookings that were not paid in time", expired);
        }
    }
}
