package kz.kimep.ticketing.catalog.service;

import static kz.kimep.ticketing.catalog.repo.EventSpecifications.hasStatus;
import static kz.kimep.ticketing.catalog.repo.EventSpecifications.inCity;
import static kz.kimep.ticketing.catalog.repo.EventSpecifications.isPublic;
import static kz.kimep.ticketing.catalog.repo.EventSpecifications.startsBefore;
import static kz.kimep.ticketing.catalog.repo.EventSpecifications.startsFrom;

import kz.kimep.ticketing.catalog.api.EventFilter;
import kz.kimep.ticketing.catalog.api.EventResponse;
import kz.kimep.ticketing.catalog.api.EventSummary;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.shared.api.PageResponse;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCatalogService {

    private final EventRepository eventRepository;

    @Cacheable(value = CacheConfig.EVENT_LIST, key = "#filter.cacheKey(#pageable)")
    @Transactional(readOnly = true)
    public PageResponse<EventSummary> list(EventFilter filter, Pageable pageable) {
        Specification<Event> spec = Specification.allOf(isPublic(), inCity(filter.city()), startsFrom(filter.from()),
                startsBefore(filter.to()), hasStatus(filter.status()));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("startsAt", "id"));
        return PageResponse.from(eventRepository.findAll(spec, sorted).map(EventSummary::from));
    }

    @Cacheable(value = CacheConfig.EVENT, key = "#id")
    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return eventRepository.findDetailedById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new NotFoundException("Event " + id + " was not found."));
    }
}
