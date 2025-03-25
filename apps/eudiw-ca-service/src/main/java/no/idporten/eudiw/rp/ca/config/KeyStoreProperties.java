package no.idporten.eudiw.rp.ca.config;

/**
 * Properties for opening a keystore and loading keys.
 */
public record KeyStoreProperties(String type, String location, String password, String keyAlias, String keyPassword) {
    public KeyStoreProperties {
        if (type == null || location == null || password == null || keyAlias == null || keyPassword == null) {
            throw new IllegalArgumentException("All fields must be non-null");
        }
    }
}
