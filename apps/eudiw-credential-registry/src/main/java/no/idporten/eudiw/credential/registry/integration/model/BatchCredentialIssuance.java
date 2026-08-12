package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class BatchCredentialIssuance {

    @JsonProperty("batch_size")
    @Min(2)
    @Max(99)
    private Integer batchSize = -1; //Litt usikker paa dette

    public BatchCredentialIssuance() {
    }

    public BatchCredentialIssuance(Integer batchSize) {
        setBatchSize(batchSize);
    }

    public Integer getBatchSize() {
        return batchSize;
    }
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
