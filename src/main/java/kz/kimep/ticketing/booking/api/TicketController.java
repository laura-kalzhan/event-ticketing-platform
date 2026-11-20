package kz.kimep.ticketing.booking.api;

import kz.kimep.ticketing.booking.service.TicketService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public TicketResponse get(@PathVariable String code, @AuthenticationPrincipal AuthenticatedUser user) {
        return ticketService.getTicket(code, user);
    }
}
