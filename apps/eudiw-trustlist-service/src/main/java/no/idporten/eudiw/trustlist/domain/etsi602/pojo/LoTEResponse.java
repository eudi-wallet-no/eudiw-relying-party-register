package no.idporten.eudiw.trustlist.domain.etsi602.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

// Wrapper for the response to ensure it includes a top-level "LoTE" property.
public record LoTEResponse(@JsonProperty("LoTE") no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE loTE) {
}
