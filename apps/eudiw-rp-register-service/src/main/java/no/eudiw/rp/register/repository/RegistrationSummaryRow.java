package no.eudiw.rp.register.repository;

import java.util.UUID;

public record RegistrationSummaryRow(UUID instanceId, String serviceTradeName, long createdMs, long lastUpdatedMs,
                                     UUID roleId, String entitlement) {
}
