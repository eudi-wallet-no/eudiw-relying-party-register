package no.idporten.eudiw.ca.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Data
public class CertificateProfile {

    @NotNull
    private List<String> extendedKeyUsage;

}
