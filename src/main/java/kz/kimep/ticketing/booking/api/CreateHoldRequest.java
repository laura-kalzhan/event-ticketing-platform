package kz.kimep.ticketing.booking.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateHoldRequest(@NotNull Long eventId, @NotEmpty List<@NotNull Long> seatIds) {
}
