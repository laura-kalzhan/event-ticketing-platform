package kz.kimep.ticketing.catalog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.kimep.ticketing.catalog.api.CreateEventRequest;
import kz.kimep.ticketing.catalog.api.EventResponse;
import kz.kimep.ticketing.catalog.api.PriceRequest;
import kz.kimep.ticketing.catalog.api.UpdateEventRequest;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.VenueRepository;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.shared.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventOrganizerService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventOwnership eventOwnership;
    private final AuditService auditService;
    private final Clock clock;

    @CacheEvict(value = CacheConfig.EVENT_LIST, allEntries = true)
    @Transactional
    public EventResponse create(CreateEventRequest request, AuthenticatedUser user) {
        checkDates(request.salesOpenAt(), request.salesCloseAt(), request.startsAt());
        Venue venue = venueRepository.findWithSectionsById(request.venueId())
                .orElseThrow(() -> new BadRequestException("Venue " + request.venueId() + " does not exist."));
        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartsAt(request.startsAt());
        event.setDoorsOpenAt(request.doorsOpenAt());
        event.setSalesOpenAt(request.salesOpenAt());
        event.setSalesCloseAt(request.salesCloseAt());
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedAt(clock.instant());
        event.setVenue(venue);
        event.setOrganizerId(user.id());
        addPrices(event, venue, request);
        eventRepository.save(event);
        auditService.record(user.id(), "EVENT_CREATED", "Event", event.getId());
        return EventResponse.from(event);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.EVENT, key = "#id"),
        @CacheEvict(value = CacheConfig.EVENT_LIST, allEntries = true)
    })
    @Transactional
    public EventResponse update(Long id, UpdateEventRequest request, AuthenticatedUser user) {
        checkDates(request.salesOpenAt(), request.salesCloseAt(), request.startsAt());
        Event event = eventOwnership.requireOwned(id, user);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartsAt(request.startsAt());
        event.setDoorsOpenAt(request.doorsOpenAt());
        event.setSalesOpenAt(request.salesOpenAt());
        event.setSalesCloseAt(request.salesCloseAt());
        auditService.record(user.id(), "EVENT_UPDATED", "Event", id);
        return EventResponse.from(eventRepository.findDetailedById(id).orElseThrow());
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.EVENT, key = "#id"),
        @CacheEvict(value = CacheConfig.EVENT_LIST, allEntries = true)
    })
    @Transactional
    public EventResponse publish(Long id, AuthenticatedUser user) {
        Event event = eventOwnership.requireOwned(id, user);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Only a draft event can be published. This event is " + event.getStatus()
                    + ".");
        }
        event.setStatus(EventStatus.ON_SALE);
        auditService.record(user.id(), "EVENT_PUBLISHED", "Event", id);
        return EventResponse.from(eventRepository.findDetailedById(id).orElseThrow());
    }

    private void addPrices(Event event, Venue venue, CreateEventRequest request) {
        Map<Long, Section> sections = venue.getSections().stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));
        for (PriceRequest priceRequest : request.prices()) {
            Section section = sections.get(priceRequest.sectionId());
            if (section == null) {
                throw new BadRequestException("Section " + priceRequest.sectionId()
                        + " is not part of venue " + venue.getId() + ".");
            }
            EventSectionPrice price = new EventSectionPrice();
            price.setSection(section);
            price.setPrice(priceRequest.price());
            price.setCurrency(priceRequest.currency().toUpperCase());
            event.addPrice(price);
        }
    }

    private void checkDates(Instant salesOpenAt, Instant salesCloseAt, Instant startsAt) {
        if (!salesOpenAt.isBefore(salesCloseAt) || salesCloseAt.isAfter(startsAt)) {
            throw new BadRequestException("Sales must open before they close, and close before the event starts.");
        }
    }
}
