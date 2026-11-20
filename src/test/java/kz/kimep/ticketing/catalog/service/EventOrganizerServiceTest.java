package kz.kimep.ticketing.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.catalog.api.CreateEventRequest;
import kz.kimep.ticketing.catalog.api.PriceRequest;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.PriceTier;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.VenueRepository;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOrganizerServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private AuditService auditService;

    private EventOrganizerService service;

    @BeforeEach
    void setUp() {
        service = new EventOrganizerService(eventRepository, venueRepository, new EventOwnership(eventRepository),
                auditService, new MutableClock(TestData.NOW));
    }

    @Test
    void organizerCannotPublishAnotherOrganizersEvent() {
        when(eventRepository.findWithVenueById(7L)).thenReturn(Optional.of(event(7L, 3L, EventStatus.DRAFT)));

        assertThatThrownBy(() -> service.publish(7L, TestData.ORGANIZER_ONE)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void adminCanPublishAnyDraftEvent() {
        Event event = event(7L, 3L, EventStatus.DRAFT);
        when(eventRepository.findWithVenueById(7L)).thenReturn(Optional.of(event));
        when(eventRepository.findDetailedById(7L)).thenReturn(Optional.of(event));

        assertThat(service.publish(7L, TestData.ADMIN).status()).isEqualTo("ON_SALE");
    }

    @Test
    void publishingAnEventThatIsAlreadyOnSaleIsAConflict() {
        when(eventRepository.findWithVenueById(7L)).thenReturn(Optional.of(event(7L, 2L, EventStatus.ON_SALE)));

        assertThatThrownBy(() -> service.publish(7L, TestData.ORGANIZER_ONE)).isInstanceOf(ConflictException.class);
    }

    @Test
    void priceForASectionOutsideTheVenueIsRejected() {
        when(venueRepository.findWithSectionsById(1L)).thenReturn(Optional.of(venue()));
        CreateEventRequest request = request(List.of(new PriceRequest(99L, new BigDecimal("1000"), "KZT")));

        assertThatThrownBy(() -> service.create(request, TestData.ORGANIZER_ONE))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void salesClosingAfterTheEventStartsIsRejected() {
        CreateEventRequest request = new CreateEventRequest("Show", null, 1L, TestData.NOW.plus(Duration.ofDays(5)),
                null, TestData.NOW, TestData.NOW.plus(Duration.ofDays(6)),
                List.of(new PriceRequest(10L, new BigDecimal("1000"), "KZT")));

        assertThatThrownBy(() -> service.create(request, TestData.ORGANIZER_ONE))
                .isInstanceOf(BadRequestException.class);
    }

    private CreateEventRequest request(List<PriceRequest> prices) {
        return new CreateEventRequest("Show", "desc", 1L, TestData.NOW.plus(Duration.ofDays(10)), null,
                TestData.NOW, TestData.NOW.plus(Duration.ofDays(9)), prices);
    }

    private Venue venue() {
        Venue venue = new Venue();
        venue.setId(1L);
        venue.setName("Almaty Arena");
        venue.setCity("Almaty");
        Section section = new Section();
        section.setId(10L);
        section.setName("Floor");
        section.setPriceTier(PriceTier.PREMIUM);
        venue.addSection(section);
        return venue;
    }

    private Event event(Long id, Long organizerId, EventStatus status) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Show");
        event.setOrganizerId(organizerId);
        event.setStatus(status);
        event.setVenue(venue());
        return event;
    }
}
