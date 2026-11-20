package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateVenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String addressLine,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50) String timezone,
        @NotEmpty List<@Valid CreateSectionRequest> sections) {
}
