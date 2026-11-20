package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import kz.kimep.ticketing.catalog.service.VenueService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public PageResponse<VenueSummary> list(@PageableDefault(size = 20) Pageable pageable) {
        return venueService.list(pageable);
    }

    @GetMapping("/{id}")
    public VenueResponse findById(@PathVariable Long id) {
        return venueService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        VenueResponse created = venueService.create(request, user);
        return ResponseEntity.created(URI.create("/api/v1/venues/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VenueResponse update(@PathVariable Long id, @Valid @RequestBody UpdateVenueRequest request,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return venueService.update(id, request, user);
    }
}
