package no.idporten.eudiw.trustlist.domain;


import no.idporten.eudiw.trustlist.domain.etsi602.ServiceDigitalIdentity;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ServiceDigitalIdentityTest {

    @Test
    void createServiceDigitalIdentitytest() throws IOException {
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity("MIIDFTCCArugAwIBAgIJAKdtiMwEfW1zMAoGCCqGSM49BAMEMGQxGDAWBgNVBGET D05UUk5PLTk5MTgyNTgyNzELMAkGA1UEBhMCbm8xDzANBgNVBAsTBkRpZ2RpcjEq MCgGA1UEAxMhZWlkYXMyc2FuZGthc3NlIFJQIEFjY2VzcyBDQSB0ZXN0MB4XDTI2 MDMwNTE0MDgxMloXDTI2MDYxMzE0MDgxMlowgYMxCzAJBgNVBAYTAk5PMSkwJwYD VQQKDCBTeW50ZXRpc2sgb3JnYW5pc2Fzam9uIDIzNDIzNDIzNDEpMCcGA1UEAwwg U29sdmVpZ3Mgc3ludGV0aXNrZSBvcmdhbmlzYXNqb24xHjAcBgNVBGEMFU5UUk5P LU5PRk9SLjIzNDIzNDIzNDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFDYuhXZ alO+cY8vBwGHJoGVI5UT5gxoNRaQAp5O5ogB1ZZzYxp7rOXfNkvnayExrVFqdaT3 RtyYOjS+iV1lOdGjggE0MIIBMDAfBgNVHSMEGDAWgBRRyAIlgDCCkTskItSb3F6h qKddzDAdBgNVHQ4EFgQUDXFAn63Idmcp6bdfBhWfGQhdSsgwDAYDVR0TAQH/BAIw ADBWBgNVHR8ETzBNMEugSaBHhkVodHRwczovL2NhLnRlc3QuZWlkYXMyc2FuZGth c3NlLm5ldC92MS9jZXJ0cy9pbnRlcm1lZGlhdGVzL2FjY2Vzcy5jcmwwYQYIKwYB BQUHAQEEVTBTMFEGCCsGAQUFBzAChkVodHRwczovL2NhLnRlc3QuZWlkYXMyc2Fu ZGthc3NlLm5ldC92MS9jZXJ0cy9pbnRlcm1lZGlhdGVzL2FjY2Vzcy5jZXIwDgYD VR0PAQH/BAQDAgWgMBUGA1UdJQEB/wQLMAkGByiBjF0FAQYwCgYIKoZIzj0EAwQD SAAwRQIhALK/Xv1BDBZk7+ruyECGUmAJjQxa/SJaUAj06Bs7RFyWAiAzrPOEKgx1 +eaZxDNKQwW2PpFFeiWjywTd/AQRsJOJrA==");
        Assertions.assertNotNull(serviceDigitalIdentity);
        Assertions.assertEquals(X509CertificateHolder.class, serviceDigitalIdentity.getCertificate().getClass());
        Assertions.assertEquals(String.class, serviceDigitalIdentity.getValidCertString().getClass());
        Assertions.assertEquals(4, serviceDigitalIdentity.getCertificate().getSubject().getAttributeTypes().length);
    }

}
