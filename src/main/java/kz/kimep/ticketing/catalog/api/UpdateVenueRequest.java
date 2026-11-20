package kz.kimep.ticketing.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String addressLine,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50) String timezone) {
}
