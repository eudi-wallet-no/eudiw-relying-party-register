package no.eudiw.rp.register.api.v2.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
    name = "registration-status-api",
    description = "Registration status operations"
)
@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class V2RegistrationStatusEndpoint {

    private static final String AUDIT_WALLET_RELYING_PARTY_DEACTIVATED = "PARTY-DEACTIVATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_ACTIVATED = "PARTY-ACTIVATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_SERVICE_DEACTIVATED = "SERVICE-DEACTIVATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_SERVICE_ACTIVATED = "SERVICE-ACTIVATED";
    private static final String AUDIT_RELYING_PARTY_INSTANCE_DEACTIVATED = "INSTANCE-DEACTIVATED";
    private static final String AUDIT_RELYING_PARTY_INSTANCE_ACTIVATED = "INSTANCE-ACTIVATED";

    private final RelyingPartyService relyingPartyService;

    @Operation(summary = "Deactivate a WalletRelyingParty and descendants")
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_DEACTIVATED)
    @PatchMapping("/parties/{id}/deactivate")
    public ResponseEntity<Void> deactivateWalletRelyingParty(@PathVariable("id") UUID walletRelyingPartyId) {
        relyingPartyService.deactivateWalletRelyingParty(walletRelyingPartyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Reactivate a WalletRelyingParty"
    )
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_ACTIVATED)
    @PatchMapping("/parties/{id}/activate")
    public ResponseEntity<Void> activateWalletRelyingParty(@PathVariable("id") UUID walletRelyingPartyId) {
        relyingPartyService.activateWalletRelyingParty(walletRelyingPartyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a WalletRelyingPartyService and its instances")
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_SERVICE_DEACTIVATED)
    @PatchMapping("/services/{id}/deactivate")
    public ResponseEntity<Void> deactivateWalletRelyingPartyService(
        @PathVariable("id") UUID walletRelyingPartyServiceId
    ) {
        relyingPartyService.deactivateWalletRelyingPartyService(walletRelyingPartyServiceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a WalletRelyingPartyService")
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_SERVICE_ACTIVATED)
    @PatchMapping("/services/{id}/activate")
    public ResponseEntity<Void> activateWalletRelyingPartyService(
        @PathVariable("id") UUID walletRelyingPartyServiceId
    ) {
        relyingPartyService.activateWalletRelyingPartyService(walletRelyingPartyServiceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a RelyingPartyInstance")
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_DEACTIVATED)
    @PatchMapping("/instances/{id}/deactivate")
    public ResponseEntity<Void> deactivateRelyingPartyInstance(@PathVariable("id") UUID relyingPartyInstanceId) {
        relyingPartyService.deactivateRelyingPartyInstance(relyingPartyInstanceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a RelyingPartyInstance")
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_ACTIVATED)
    @PatchMapping("/instances/{id}/activate")
    public ResponseEntity<Void> activateRelyingPartyInstance(@PathVariable("id") UUID relyingPartyInstanceId) {
        relyingPartyService.activateRelyingPartyInstance(relyingPartyInstanceId);
        return ResponseEntity.noContent().build();
    }
}
