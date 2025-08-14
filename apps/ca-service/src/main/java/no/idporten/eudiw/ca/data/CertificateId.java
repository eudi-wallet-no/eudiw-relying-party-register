package no.idporten.eudiw.ca.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CertificateId implements Serializable {

    private String serialNo;
    private String issuerCa;

}
