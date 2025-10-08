package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyCsrResource(
    @JsonProperty(value = "csr", required = true)
    @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
    PKCS10CertificationRequest csr
) { }
