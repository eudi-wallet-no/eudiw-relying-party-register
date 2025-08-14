package no.idporten.eudiw.ca.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When handling certificate serial numbers")
public class SerialNumberUtilsTest {

    @DisplayName("then serial numbers can be converted between BigInteger and String")
    @Test
    void testConvertSerialNumbers() {
        BigInteger serialNumber = SerialNumberUtils.generateSerialNumber();
        String convertedSerialNumber = SerialNumberUtils.convertToString(serialNumber);
        BigInteger parsedSerialNumber = SerialNumberUtils.convertFromString(convertedSerialNumber);
        assertEquals(serialNumber, parsedSerialNumber);
    }

}
