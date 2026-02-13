package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ValueSerializer;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyCsrResource(
    @JsonProperty(value = "csr", required = true)
    @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
    PKCS10CertificationRequest csr
) { }
