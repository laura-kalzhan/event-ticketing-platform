package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull Long venueId,
        @NotNull @Future Instant startsAt,
        Instant doorsOpenAt,
        @NotNull Instant salesOpenAt,
        @NotNull Instant salesCloseAt,
        @NotEmpty List<@Valid PriceRequest> prices) {
}
