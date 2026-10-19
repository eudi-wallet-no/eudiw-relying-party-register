package no.idporten.eudiw.ca.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Validated
@Data
public class CertificateProfile {

    @NotNull
    private List<String> extendedKeyUsage = new ArrayList<>();

    private boolean extendedKeyUsageCritical;

    @NotNull
    private List<String> certificatePolicies = new ArrayList<>();

    private boolean certificatePoliciesCritical;

    @NotNull
    private List<URI> subjectAltNames = new ArrayList<>();

    private QCStatements qcStatements = new QCStatements();

}
