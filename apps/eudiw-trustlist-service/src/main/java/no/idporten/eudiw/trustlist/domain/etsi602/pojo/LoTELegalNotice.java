package no.idporten.eudiw.trustlist.domain.etsi602.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.trustlist.etsi119602.pojo.MultiLangString;

// This class is missing from json schema generation, but should be included!
public record LoTELegalNotice(@JsonProperty("LoTELegalNotice") MultiLangString loTELegalNotice) {
}
