package no.idporten.eudiw.ca.data;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Utility for serial number conversions.  BigInteger in JCA, Varchar in databases,
 */
public class SerialNumberUtils {

    private SerialNumberUtils() {}

    public static String convertToString(BigInteger serialNumber) {
        return serialNumber.toString(10);
    }

    public static BigInteger convertFromString(String serialNumber) {
        return new BigInteger(serialNumber);
    }

    public static BigInteger generateSerialNumber() {
        // Using current time in milliseconds as a base
        long timeMillis = System.currentTimeMillis();
        // Adding a random component to enhance uniqueness
        BigInteger randomPart = new BigInteger(64, new SecureRandom()).abs(); // 64-bit random number
        return BigInteger.valueOf(timeMillis).add(randomPart);
    }

}
