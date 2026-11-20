package kz.kimep.ticketing.booking.api;

import jakarta.validation.Valid;
import java.net.URI;
import kz.kimep.ticketing.booking.service.HoldService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<HoldResponse> create(@Valid @RequestBody CreateHoldRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        HoldResponse hold = holdService.createHold(user, request);
        return ResponseEntity.created(URI.create("/api/v1/holds/" + hold.holdReference())).body(hold);
    }

    @GetMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public HoldResponse get(@PathVariable String reference, @AuthenticationPrincipal AuthenticatedUser user) {
        return holdService.getHold(reference, user);
    }

    @DeleteMapping("/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void release(@PathVariable String reference, @AuthenticationPrincipal AuthenticatedUser user) {
        holdService.releaseHold(reference, user);
    }
}
