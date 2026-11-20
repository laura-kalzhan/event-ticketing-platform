package kz.kimep.ticketing.booking.api;

import jakarta.validation.Valid;
import java.net.URI;
import kz.kimep.ticketing.booking.service.BookingLifecycleService;
import kz.kimep.ticketing.booking.service.BookingService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingLifecycleService bookingLifecycleService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        BookingResponse booking = bookingService.createBooking(user, request);
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + booking.bookingReference())).body(booking);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<BookingSummary> listMine(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return bookingService.listMine(user, pageable);
    }

    @GetMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse get(@PathVariable String reference, @AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.getBooking(reference, user);
    }

    @PostMapping("/{reference}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void cancel(@PathVariable String reference, @AuthenticationPrincipal AuthenticatedUser user) {
        bookingLifecycleService.cancel(reference, user);
    }
}
