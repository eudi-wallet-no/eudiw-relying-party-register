package no.idporten.eudiw.ca.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigInteger;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CertificateId implements Serializable {

    private BigInteger serialNo;
    private String issuerCa;

}
