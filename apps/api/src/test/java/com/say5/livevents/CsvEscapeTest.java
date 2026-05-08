package com.say5.livevents;

import com.say5.livevents.api.CsvEscape;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvEscapeTest {

    @Test
    void plainFieldsAreNotQuoted() {
        assertThat(CsvEscape.escape("hello")).isEqualTo("hello");
    }

    @Test
    void emptyStringStaysEmpty() {
        assertThat(CsvEscape.escape("")).isEmpty();
    }

    @Test
    void nullBecomesEmptyField() {
        assertThat(CsvEscape.escape(null)).isEmpty();
    }

    @Test
    void commasForceQuoting() {
        assertThat(CsvEscape.escape("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void embeddedQuotesAreDoubled() {
        assertThat(CsvEscape.escape("a\"b")).isEqualTo("\"a\"\"b\"");
    }

    @Test
    void newlinesForceQuoting() {
        assertThat(CsvEscape.escape("a\nb")).isEqualTo("\"a\nb\"");
    }

    @Test
    void rowJoinsWithCommas() {
        assertThat(CsvEscape.row("a", "b", "c")).isEqualTo("a,b,c");
    }

    @Test
    void rowQuotesOnlyFieldsThatNeedIt() {
        assertThat(CsvEscape.row("plain", "with,comma", "ok"))
                .isEqualTo("plain,\"with,comma\",ok");
    }
}
