package no.idporten.eudiw.trustlist.domain.etsi602.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.trustlist.etsi119602.pojo.NonEmptyMultiLangURI;

// This class is missing from json schema generation, but should be included!
public record LoTEPolicy(@JsonProperty("LoTEPolicy") NonEmptyMultiLangURI loTEPolicy) {
}
