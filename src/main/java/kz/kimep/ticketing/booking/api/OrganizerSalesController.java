package kz.kimep.ticketing.booking.api;

import kz.kimep.ticketing.booking.service.SalesService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizer/events")
@RequiredArgsConstructor
public class OrganizerSalesController {

    private final SalesService salesService;

    @GetMapping("/{id}/sales")
    @PreAuthorize("hasRole('ORGANIZER')")
    public SalesSummary sales(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return salesService.summary(id, user);
    }
}
