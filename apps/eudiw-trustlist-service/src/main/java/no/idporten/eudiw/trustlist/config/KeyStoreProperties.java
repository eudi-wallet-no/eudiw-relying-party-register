package no.idporten.eudiw.trustlist.config;

import jakarta.validation.constraints.NotNull;

/**
 * Properties for opening a keystore and loading private keys.
 */
public record KeyStoreProperties(@NotNull String type, @NotNull String location, @NotNull String password, @NotNull String keyAlias, @NotNull String keyPassword) {
}
