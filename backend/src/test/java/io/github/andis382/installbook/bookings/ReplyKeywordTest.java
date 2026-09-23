package io.github.andis382.installbook.bookings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReplyKeywordTest {

    @Test
    void readsTheWayPeopleActuallyAnswer() {
        assertThat(ReplyHandler.keyword(" Po! ")).isEqualTo("po");
        assertThat(ReplyHandler.keyword("YES.")).isEqualTo("yes");
        assertThat(ReplyHandler.keyword("1")).isEqualTo("1");
        assertThat(ReplyHandler.keyword("Rezervo")).isEqualTo("rezervo");
        assertThat(ReplyHandler.keyword("NDALO")).isEqualTo("ndalo");
        assertThat(ReplyHandler.keyword(null)).isEmpty();
    }

    @Test
    void bookingAndStopWordsDoNotOverlap() {
        assertThat(ReplyHandler.BOOK).doesNotContainAnyElementsOf(ReplyHandler.STOP);
        assertThat(ReplyHandler.BOOK).contains("1", "po", "yes", "book", "rezervo");
        assertThat(ReplyHandler.STOP).contains("stop", "ndalo");
    }

    @Test
    void aSentenceIsNotAKeyword() {
        assertThat(ReplyHandler.BOOK).doesNotContain(ReplyHandler.keyword("Po, por jo këtë muaj"));
    }
}
