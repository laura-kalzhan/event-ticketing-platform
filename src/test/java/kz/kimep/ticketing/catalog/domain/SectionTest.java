package kz.kimep.ticketing.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SectionTest {

    @Test
    void rowLabelsContinueAfterZ() {
        assertThat(Section.rowLabel(0)).isEqualTo("A");
        assertThat(Section.rowLabel(25)).isEqualTo("Z");
        assertThat(Section.rowLabel(26)).isEqualTo("AA");
        assertThat(Section.rowLabel(27)).isEqualTo("AB");
    }

    @Test
    void generatedSeatsCoverEveryRowAndNumber() {
        Section section = new Section();
        section.setRowCount(3);
        section.setSeatsPerRow(4);

        section.generateSeats();

        assertThat(section.getSeats()).hasSize(12);
        assertThat(section.getSeats().get(0).getRowLabel()).isEqualTo("A");
        assertThat(section.getSeats().get(11).getRowLabel()).isEqualTo("C");
        assertThat(section.getSeats().get(11).getSeatNumber()).isEqualTo(4);
    }
}
