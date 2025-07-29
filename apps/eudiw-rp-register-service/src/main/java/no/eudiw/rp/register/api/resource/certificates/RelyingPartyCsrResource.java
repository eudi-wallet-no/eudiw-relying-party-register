package no.eudiw.rp.register.api.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.With;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyCsrResource(
    @JsonProperty(value = "csr", required = true)
    @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
    @JsonDeserialize(using = PKCS10CertificationRequestJsonDeserializer.class)
    PKCS10CertificationRequest csr
) { }
