package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBException;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class SignedTrustlist612Service {

    private final Trustlist612GeneratorService trustListGeneratorService;
    private final XMLSignerService xmlSignerService;
    private final Logger log = LoggerFactory.getLogger(SignedTrustlist612Service.class);

    private volatile String trustlist;
    private volatile String sha2;

    // Only generate trustlist once at application startup
    @PostConstruct
    public void initTrustlist() {
        try {
            this.trustlist = generateTrustServiceStatusList();
        } catch (ApplicationException | SigningException e) {
            log.error("Failed to generate Trust Service Status List on startup", e);
            throw e;
        }
        if (this.trustlist != null) {
            try {
                this.sha2 = generateShaOfTrustlist();
            } catch (ApplicationException e) {
                log.error("Failed to generate SHA-256 hash of Trust Service Status List on startup", e);
                throw e;
            }
        }
    }

    public SignedTrustlist612Service(Trustlist612GeneratorService trustListGeneratorService, XMLSignerService xmlSignerService) {
        this.trustListGeneratorService = trustListGeneratorService;
        this.xmlSignerService = xmlSignerService;
    }

    private String generateTrustServiceStatusList() {
        TrustServiceStatusList trustServiceStatusList = trustListGeneratorService.generateTrustServiceStatusList();
        Document document;
        try {
            document = XMLUtils.parseTrustlist(trustServiceStatusList);
        } catch (JAXBException e) {
            throw new ApplicationException("Failed to parse Trustlist", e);
        }
        Document signedDocument = xmlSignerService.createEnvelopedSignature(document);
        try {
            return XMLUtils.formatXml(signedDocument);
        } catch (TransformerException e) {
            throw new ApplicationException("Failed to format signed Document of Trustlist to xml", e);
        }
    }

    public String getTrustlist() {
        if (trustlist == null) {
            log.warn("Trustlist is not initialized, try generating again.");
            trustlist = generateTrustServiceStatusList();
        }
        return trustlist;
    }

    public String getSha2() {
        if (sha2 == null) {
            log.warn("SHA-256 hash of Trustlist is not initialized, try generating again.");
            sha2 = generateShaOfTrustlist();
        }
        return sha2;
    }

    public String generateShaOfTrustlist() {
        String sha2 = generateShaOfString(getTrustlist());
        log.info("Generated SHA-256 hex of trustlist: {}", sha2);
        return sha2;
    }

    protected String generateShaOfString(String value) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new ApplicationException("Failed to get MessageDigest for SHA-256 alg", e);
        }
        byte[] hashedValue = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashedValue);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
