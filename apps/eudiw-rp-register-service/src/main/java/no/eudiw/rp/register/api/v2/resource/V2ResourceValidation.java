package no.eudiw.rp.register.api.v2.resource;

import java.net.URI;

public final class V2ResourceValidation {

    private V2ResourceValidation() { }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isAbsoluteUri(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            return URI.create(value).isAbsolute();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
