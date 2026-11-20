package kz.kimep.ticketing.booking.api;

import jakarta.validation.constraints.NotBlank;

public record CreateBookingRequest(@NotBlank String holdReference) {
}
