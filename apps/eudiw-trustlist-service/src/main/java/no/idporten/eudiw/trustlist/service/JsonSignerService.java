package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.KeyProvider;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import no.idporten.eudiw.trustlist.exception.JsonSignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.List;

@Service
public class JsonSignerService {

    private String signedTrustlist;

    private final TrustListACAGeneratorService generatorService;

    private final KeyProvider keyProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    private final static Logger log = LoggerFactory.getLogger(JsonSignerService.class);

    public JsonSignerService(TrustListACAGeneratorService generatorService, KeyProvider keyProvider) {
        this.generatorService = generatorService;
        this.keyProvider = keyProvider;
    }

    public String getSignedTrustlist() {
        if (this.signedTrustlist == null) {
            log.warn("ACA trustlist is not initialized, try generating again.");
            this.signedTrustlist = signedJson();
        }
        return this.signedTrustlist;
    }

    protected String signedJson() {
        LoTE loTE = generatorService.generateTrustlistACA();
        String loteType = getListType(loTE); // For logging/errorhandling messages
        String json = convertLoTEtoJsonString(loTE, loteType);
        return signJson(json, loteType);
    }

    private String getListType(LoTE loTE) {
        if (loTE.getListAndSchemeInformation() == null || loTE.getListAndSchemeInformation().getLoTEType() == null) {
            LoggerFactory.getLogger(getClass()).warn("LoTE type is null or getListAndSchemeInformation==null, use empty string as default type for trustlist");
            return "";
        }
        return loTE.getListAndSchemeInformation().getLoTEType().toString();
    }

    private String convertLoTEtoJsonString(LoTE loTE, String loteType) {

        try {
            // TODO: Dytt inn mere konfig for aa fjerne tomme felter i json
            return mapper.writeValueAsString(loTE);
        } catch (JacksonException e) {
            throw new JsonSignException("Failed to serialize LoTE to JSON for trustlist %s".formatted(loteType), e);
        }
    }

    private String signJson(String json, String loteType) {

        List<Base64> certBase64chain = getCertificateChainFromKeystore(loteType);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS512)
                .x509CertChain(certBase64chain)
                .customParam("iat", Instant.now().getEpochSecond())
                .build();

        Payload payload = new Payload(json);
        JWSObject jwsObject = new JWSObject(header, payload);

        PrivateKey privateKey = keyProvider.getPrivateKey();
        JWSSigner signer = new RSASSASigner(privateKey);

        try {
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            throw new JsonSignException("Failed signing trustlist %s".formatted(loteType), e);
        }
        return jwsObject.serialize();
    }

    private List<Base64> getCertificateChainFromKeystore(String loteType) {
        List<Certificate> certificateChain = keyProvider.getCertificateChain();
        return certificateChain.stream().map(certificate -> certificateToBase64(certificate, loteType)).toList();
    }

    private Base64 certificateToBase64(Certificate certificate, Object loteType) {
        try {
            return Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new JsonSignException("Failed to get certificateChain from keystore for trustlist %s".formatted(loteType), e);
        }
    }

    // Only generate ACA trustlist once at application startup
    @PostConstruct
    public void initTrustlist() {
        try {
            this.signedTrustlist = getSignedTrustlist();
        } catch (ApplicationException e) {
            log.error("Failed to generate ACA Trust Service Status List on startup", e);
            throw e;
        }

    }
}
