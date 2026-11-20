package kz.kimep.ticketing.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PriceRequest(
        @NotNull Long sectionId,
        @NotNull @Positive BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
