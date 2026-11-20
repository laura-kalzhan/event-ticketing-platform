package kz.kimep.ticketing.catalog.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.kimep.ticketing.catalog.domain.PriceTier;

public record CreateSectionRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(1) @Max(50) int rowCount,
        @Min(1) @Max(100) int seatsPerRow,
        @NotNull PriceTier priceTier) {
}
