package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.api.CreateSectionRequest;
import kz.kimep.ticketing.catalog.api.CreateVenueRequest;
import kz.kimep.ticketing.catalog.api.UpdateVenueRequest;
import kz.kimep.ticketing.catalog.api.VenueResponse;
import kz.kimep.ticketing.catalog.api.VenueSummary;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.VenueRepository;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.api.PageResponse;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<VenueSummary> list(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return PageResponse.from(venueRepository.findAll(sorted).map(VenueSummary::from));
    }

    @Cacheable(value = CacheConfig.VENUE, key = "#id")
    @Transactional(readOnly = true)
    public VenueResponse findById(Long id) {
        return VenueResponse.from(findVenue(id));
    }

    @Transactional
    public VenueResponse create(CreateVenueRequest request, AuthenticatedUser user) {
        Venue venue = new Venue();
        venue.setName(request.name());
        venue.setAddressLine(request.addressLine());
        venue.setCity(request.city());
        venue.setTimezone(request.timezone());
        for (CreateSectionRequest sectionRequest : request.sections()) {
            Section section = new Section();
            section.setName(sectionRequest.name());
            section.setRowCount(sectionRequest.rowCount());
            section.setSeatsPerRow(sectionRequest.seatsPerRow());
            section.setPriceTier(sectionRequest.priceTier());
            section.generateSeats();
            venue.addSection(section);
        }
        venueRepository.save(venue);
        auditService.record(user.id(), "VENUE_CREATED", "Venue", venue.getId());
        return VenueResponse.from(venue);
    }

    @CacheEvict(value = CacheConfig.VENUE, key = "#id")
    @Transactional
    public VenueResponse update(Long id, UpdateVenueRequest request, AuthenticatedUser user) {
        Venue venue = findVenue(id);
        venue.setName(request.name());
        venue.setAddressLine(request.addressLine());
        venue.setCity(request.city());
        venue.setTimezone(request.timezone());
        auditService.record(user.id(), "VENUE_UPDATED", "Venue", id);
        return VenueResponse.from(venue);
    }

    private Venue findVenue(Long id) {
        return venueRepository.findWithSectionsById(id)
                .orElseThrow(() -> new NotFoundException("Venue " + id + " was not found."));
    }
}
