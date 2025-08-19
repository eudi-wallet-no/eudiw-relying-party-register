package no.idporten.eudiw.rp.admin.web.search.resultsview;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@SuppressWarnings("unused") // component used in Thymeleaf
public class TimestampFormatter {

    private static final ZoneId DEFAULT_ZONEID = ZoneId.of("Europe/Oslo");

    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss z");

    public String formatEpochMillis(long millis, ZoneId zoneId) {
        return Instant.ofEpochMilli(millis)
                      .atZone(zoneId)
                      .format(DATETIME_FORMATTER);
    }
    @SuppressWarnings("unused") // method used in Thymeleaf
    public String formatEpochMillis(long millis) {
        return formatEpochMillis(millis, DEFAULT_ZONEID);
    }
}
