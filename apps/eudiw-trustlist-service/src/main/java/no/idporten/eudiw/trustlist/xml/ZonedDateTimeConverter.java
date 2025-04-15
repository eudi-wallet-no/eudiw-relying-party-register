package no.idporten.eudiw.trustlist.xml;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JAXB date format handling.
 */
public class ZonedDateTimeConverter {

    public static ZonedDateTime parseDateTime(String inputDate)  {
        return inputDate != null ? DateTimeFormatter.ISO_INSTANT.parse(inputDate, ZonedDateTime::from) : null;
    }

    public static String printDateTime(ZonedDateTime inputDate) {
        return inputDate != null ? DateTimeFormatter.ISO_INSTANT.format(inputDate) : null;
    }

}
