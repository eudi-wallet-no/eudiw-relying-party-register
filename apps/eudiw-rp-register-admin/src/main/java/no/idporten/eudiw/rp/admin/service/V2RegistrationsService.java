package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient.Registration;
import no.idporten.eudiw.rp.admin.web.resource.RegistrationSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class V2RegistrationsService {

    private final V2RegisterClient registerClient;

    public List<RegistrationSummary> getAllByOrgno(String orgno) {
        if (orgno == null || orgno.isBlank()) {
            throw new IllegalArgumentException("Organization number is required");
        }

        List<Registration> registrations = registerClient.getRegistrations(orgno);
        return registrations.stream().map(RegistrationSummary::from).toList();
    }
}
