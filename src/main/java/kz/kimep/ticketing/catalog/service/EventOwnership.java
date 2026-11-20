package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventOwnership {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public Event requireOwned(Long eventId, AuthenticatedUser user) {
        return eventRepository.findWithVenueById(eventId)
                .filter(event -> user.isAdmin() || event.isOrganizedBy(user.id()))
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " was not found."));
    }
}
