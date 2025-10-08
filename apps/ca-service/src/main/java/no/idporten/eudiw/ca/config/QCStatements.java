package no.idporten.eudiw.ca.config;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class QCStatements {

    private String qcType;

}
