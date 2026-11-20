package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import kz.kimep.ticketing.catalog.service.EventCatalogService;
import kz.kimep.ticketing.catalog.service.EventOrganizerService;
import kz.kimep.ticketing.catalog.service.SeatMapService;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCatalogService eventCatalogService;
    private final EventOrganizerService eventOrganizerService;
    private final SeatMapService seatMapService;

    @GetMapping
    public PageResponse<EventSummary> list(@ModelAttribute EventFilter filter,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return eventCatalogService.list(filter, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return eventCatalogService.findById(id);
    }

    @GetMapping("/{id}/availability")
    public List<SectionAvailability> availability(@PathVariable Long id) {
        return seatMapService.availability(id);
    }

    @GetMapping("/{id}/seats")
    public PageResponse<SectionSeatMap> seats(@PathVariable Long id, @PageableDefault(size = 1) Pageable pageable) {
        return seatMapService.seatMap(id, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        EventResponse created = eventOrganizerService.create(request, user);
        return ResponseEntity.created(URI.create("/api/v1/events/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return eventOrganizerService.update(id, request, user);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse publish(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return eventOrganizerService.publish(id, user);
    }
}
