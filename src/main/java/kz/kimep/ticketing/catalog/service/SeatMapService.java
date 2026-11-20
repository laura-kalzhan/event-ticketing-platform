package kz.kimep.ticketing.catalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kz.kimep.ticketing.catalog.api.SeatView;
import kz.kimep.ticketing.catalog.api.SectionAvailability;
import kz.kimep.ticketing.catalog.api.SectionSeatMap;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.catalog.repo.SectionRepository;
import kz.kimep.ticketing.catalog.repo.SectionSeatCount;
import kz.kimep.ticketing.shared.api.PageResponse;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatMapService {

    private static final int MAX_SECTIONS_PER_PAGE = 3;

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final SeatStatusSource seatStatusSource;

    @Cacheable(value = CacheConfig.AVAILABILITY, key = "#eventId")
    @Transactional(readOnly = true)
    public List<SectionAvailability> availability(Long eventId) {
        Event event = findEvent(eventId);
        Set<Long> taken = seatStatusSource.occupancy(eventId).allTaken();
        Map<Long, Long> takenBySection = taken.isEmpty() ? Map.of() : seatRepository.countBySeatIds(taken).stream()
                .collect(Collectors.toMap(SectionSeatCount::getSectionId, SectionSeatCount::getSeatCount));
        return seatRepository.countByVenue(event.getVenue().getId()).stream()
                .map(count -> new SectionAvailability(count.getSectionId(), count.getSectionName(),
                        count.getSeatCount(), count.getSeatCount() - takenBySection.getOrDefault(count.getSectionId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SectionSeatMap> seatMap(Long eventId, Pageable pageable) {
        Event event = findEvent(eventId);
        int size = Math.min(pageable.getPageSize(), MAX_SECTIONS_PER_PAGE);
        Page<Section> sections = sectionRepository.findByVenueIdOrderById(event.getVenue().getId(),
                PageRequest.of(pageable.getPageNumber(), size));
        List<Long> sectionIds = sections.map(Section::getId).toList();
        Map<Long, List<Seat>> seatsBySection = sectionIds.isEmpty() ? Map.of()
                : seatRepository.findBySectionIds(sectionIds).stream()
                        .collect(Collectors.groupingBy(seat -> seat.getSection().getId(), LinkedHashMap::new,
                                Collectors.toList()));
        SeatOccupancy occupancy = seatStatusSource.occupancy(eventId);
        return PageResponse.from(sections.map(section -> new SectionSeatMap(section.getId(), section.getName(),
                seatsBySection.getOrDefault(section.getId(), List.of()).stream()
                        .map(seat -> SeatView.of(seat, occupancy))
                        .toList())));
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " was not found."));
    }
}
