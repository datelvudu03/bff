package cz.kb.leon.bff.servicing.service;


import cz.kb.leon.bff.servicing.configuration.ZoneIdProperties;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeParseException;

@Setter
@Component
@RequiredArgsConstructor
@Slf4j
public class TimeService {

    @Value("${cob.start}")
    private String cobStart;

    @Value("${cob.end}")
    private String cobEnd;

    public static final String ZONE_UTC = "UTC";

    private final ZoneIdProperties zoneIdProperties;

    public ZonedDateTime parseStringToPragueZonedTime(String inputLocalTime) {
        if (inputLocalTime == null) {
            return null;
        }

        return LocalDateTime.of(LocalDate.now(), LocalTime.parse(inputLocalTime)).atZone(zoneIdProperties.getPrague());
    }

    public ZonedDateTime getPragueZonedDateTimeNow() {
        return ZonedDateTime.now(zoneIdProperties.getPrague());
    }

    public Pair<LocalTime, LocalTime> cobParsing(){
        LocalTime parsedCobStart;
        LocalTime parsedCobEnd;
        try {
            parsedCobStart = getLocalTimeUTC(parseStringToPragueZonedTime(cobStart));
            parsedCobEnd = getLocalTimeUTC(parseStringToPragueZonedTime(cobEnd));
            log.info("Parsed cob start time: {}, parsed cob end time {}", parsedCobStart, parsedCobEnd);
        } catch (DateTimeParseException e) {
            log.error("The cobStart {} or cobEnd {} is invalid.", cobStart, cobEnd, e);
            throw new DomainException(DomainExceptionCode.INVALID_DATE_FORMAT, ObjectUtil.evaluateMessage("The values for cobStart {} or cobEnd {} is invalid. Correct example: cobStart/cobEnd -> 19:00:00", cobStart, cobEnd));
        }

        if (parsedCobStart.isAfter(parsedCobEnd)) {
            throw new DomainException(DomainExceptionCode.INVALID_DATE_FORMAT, ObjectUtil.evaluateMessage("cobStart {} is before cobEnd {}", cobStart, cobEnd));
        }
        return Pair.of(parsedCobStart, parsedCobEnd);
    }

    public LocalTime getLocalTimeUTC(ZonedDateTime inputTime) {
        return inputTime.withZoneSameInstant(ZoneId.of(TimeService.ZONE_UTC)).toLocalTime();
    }


}
