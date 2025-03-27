package cz.kb.leon.bff.servicing.util;

import cz.kb.leon.bff.servicing.contract.ContractTest;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.service.TimeService;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeServiceTest extends ContractTest {

    @Autowired
    private TimeService timeService;

    @BeforeEach
    void setup(){
        timeService.setCobEnd("00:30:00");
    }

    @Test
    void parsingCodFromPropertiesTest(){
        Pair<LocalTime, LocalTime> startAndEnd = timeService.cobParsing();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(startAndEnd.getLeft()).isEqualTo(LocalTime.parse("23:00:00"));
            softly.assertThat(startAndEnd.getRight()).isEqualTo(LocalTime.parse("23:30:00"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"42342433", "10:00:00"})
    void cobParsing_error_while_parsing(String arg){
        timeService.setCobEnd(arg);
        assertThrows(DomainException.class, () -> timeService.cobParsing());
    }

    @Test
    void parseStringToPragueZonedTimeTest(){
        SoftAssertions.assertSoftly(softly -> softly.assertThat(timeService.parseStringToPragueZonedTime(null)).isNull());
    }

}
