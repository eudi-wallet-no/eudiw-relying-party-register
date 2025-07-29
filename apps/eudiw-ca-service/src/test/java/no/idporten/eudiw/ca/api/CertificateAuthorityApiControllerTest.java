package no.idporten.eudiw.ca.api;

import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.util.CertificateEncodingUtils;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("When using the Certificate Authority API")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest
public class CertificateAuthorityApiControllerTest {

    @BeforeEach
    void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Autowired
    private CertificateAuthorities certificateAuthorities;

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("then the root certificate can be downloaded")
    @Test
    void testGetRootCertificate() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/certs/root.cer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkix-cert"))
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        X509CertificateHolder certificate = new X509CertificateHolder(content);
        assertEquals(certificate.getIssuer(), certificate.getSubject());
    }

    @DisplayName("then the root certificate's CRL can be downloaded")
    @Test
    void testGetRootCertificateCRL() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/certs/root.crl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkix-crl"))
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        X509CRLHolder crl = new X509CRLHolder(content);
        assertTrue(crl.getRevokedCertificates().isEmpty());
    }

    @DisplayName("then the intermediate CA certificates can be downloaded in binary format")
    @Test
    void testGetIntermediateAccessCertificate() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/certs/intermediates/access.cer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkix-cert"))
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        X509CertificateHolder certificate = new X509CertificateHolder(content);
        assertNotEquals(certificate.getIssuer(), certificate.getSubject());
    }

    @DisplayName("then the intermediate CA certificates can be downloaded in PEM format")
    @Test
    void testGetIntermediateAccessCertificatePEM() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/certs/intermediates/access.pem"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/x-pem-file;charset=UTF-8"))
                .andReturn();
        X509CertificateHolder certificate = CertificateEncodingUtils.decodeFromPem(result.getResponse().getContentAsString(), X509CertificateHolder.class);
        assertNotEquals(certificate.getIssuer(), certificate.getSubject());
    }

    @DisplayName("then the intermediate CA certificates' CRL can be downloaded")
    @Test
    void testGetIntermediateAccessCertificateCRL() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/certs/intermediates/access.crl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkix-crl"))
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        X509CRLHolder crl = new X509CRLHolder(content);
        assertTrue(crl.getRevokedCertificates().isEmpty());
    }

    @DisplayName("then RP access certificates can be signed")
    @Test
    void testSignLeafCertificate() throws Exception {
        String csr = """
                -----BEGIN NEW CERTIFICATE REQUEST-----
                MIIBbTCCARQCAQAwXzELMAkGA1UEBhMCbm8xDTALBgNVBAgTBFNvZ24xEjAQBgNV
                BAcTCUxlaWthbmdlcjEPMA0GA1UEChMGRGlnZGlyMQ4wDAYDVQQLEwVFVURJVzEM
                MAoGA1UEAxMDcnAyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELKyeEr6OlEgW
                E0cRI3aCgzRnPu9IjoYCsPuV53/QwBe0pymYVafMPssBqiLEyuylH/AQ3Teltq66
                L96/KVs1bqBTMFEGCSqGSIb3DQEJDjFEMEIwHQYDVR0OBBYEFFLDKogDLA5GDhgY
                oiRDkMpjeQDNMCEGA1UdEQQaMBiCFmp1bml0LnJwMS5pZHBvcnRlbi5kZXYwCgYI
                KoZIzj0EAwMDRwAwRAIgVIhOFcOMK0KR9MvK3a76Hgma6susPfXDJ+HfZZe50N8C
                IF5nyI5eYXYbBBQvdAZFJStX4YgEc+7j/QV3BlIGz2HE
                -----END NEW CERTIFICATE REQUEST-----""";
        MvcResult result = mockMvc.perform(post("/v1/certs/access/991825827")
                        .header("X-API-KEY", "junit-api-key")
                        .contentType("application/x-pem-file")
                        .content(csr))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/x-pem-file;charset=UTF-8"))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        X509CertificateHolder certificateHolder = CertificateEncodingUtils.decodeFromPem(content, X509CertificateHolder.class);
        X509Certificate certificate = CertificateEncodingUtils.toX509Certificate(certificateHolder);
        certificate.verify(certificateAuthorities.findIntermediate("access").getPublicKey());
    }

    @DisplayName("then issuer certificates can be signed")
    @Test
    void testSignIssuerCertificate() throws Exception {
        String csr = """
                -----BEGIN NEW CERTIFICATE REQUEST-----
                MIIBbTCCARQCAQAwXzELMAkGA1UEBhMCbm8xDTALBgNVBAgTBFNvZ24xEjAQBgNV
                BAcTCUxlaWthbmdlcjEPMA0GA1UEChMGRGlnZGlyMQ4wDAYDVQQLEwVFVURJVzEM
                MAoGA1UEAxMDcnAyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELKyeEr6OlEgW
                E0cRI3aCgzRnPu9IjoYCsPuV53/QwBe0pymYVafMPssBqiLEyuylH/AQ3Teltq66
                L96/KVs1bqBTMFEGCSqGSIb3DQEJDjFEMEIwHQYDVR0OBBYEFFLDKogDLA5GDhgY
                oiRDkMpjeQDNMCEGA1UdEQQaMBiCFmp1bml0LnJwMS5pZHBvcnRlbi5kZXYwCgYI
                KoZIzj0EAwMDRwAwRAIgVIhOFcOMK0KR9MvK3a76Hgma6susPfXDJ+HfZZe50N8C
                IF5nyI5eYXYbBBQvdAZFJStX4YgEc+7j/QV3BlIGz2HE
                -----END NEW CERTIFICATE REQUEST-----""";
        MvcResult result = mockMvc.perform(post("/v1/certs/issuer/991825827")
                        .header("X-API-KEY", "junit-api-key")
                        .contentType("application/x-pem-file")
                        .content(csr))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/x-pem-file;charset=UTF-8"))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        X509CertificateHolder certificateHolder = CertificateEncodingUtils.decodeFromPem(content, X509CertificateHolder.class);
        X509Certificate certificate = CertificateEncodingUtils.toX509Certificate(certificateHolder);
        certificate.verify(certificateAuthorities.findIntermediate("issuer").getPublicKey());
    }

}
