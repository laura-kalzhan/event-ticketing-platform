package kz.kimep.ticketing.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull Instant startsAt,
        Instant doorsOpenAt,
        @NotNull Instant salesOpenAt,
        @NotNull Instant salesCloseAt) {
}
