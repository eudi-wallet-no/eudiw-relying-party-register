package no.idporten.eudiw.ca.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.ca.config.CertificateAuthority;
import no.idporten.eudiw.ca.config.CertificateProfile;
import no.idporten.eudiw.ca.data.Certificate;
import no.idporten.eudiw.ca.data.CertificateRepository;
import no.idporten.eudiw.ca.data.SerialNumberUtils;
import no.idporten.eudiw.ca.exception.CertificateAuthorityException;
import no.idporten.eudiw.ca.util.CertificateEncodingUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CertificateAuthorityService {

    public static final String DIGDIR_ORGNO = "991825827";
    public static final String OID_ORGANIZATION_NUMBER = "2.5.4.97";
    public static final String SIGNATURE_ALGORITHM_SHA_512_WITH_ECDSA = "SHA512WITHECDSA";

    private final CertificateRepository certificateRepository;

    /**
     * Decodes CSR from PEM input.  Throws exception for empty or invalid input.
     * @param csr csr
     * @return decoded csr
     */
    public PKCS10CertificationRequest decodeCsr(String csr) {
        if (! StringUtils.hasText(csr)) {
            throw new CertificateAuthorityException("invalid_request", "CSR is empty", HttpStatus.BAD_REQUEST);
        }
        return CertificateEncodingUtils.decodeFromPem(csr, PKCS10CertificationRequest.class);
    }

    /**
     * Validates csr
     *
     * @param csr signed csr
     * @throws Exception if CSR is not valid
     */
    protected void validateCSR(PKCS10CertificationRequest csr) throws Exception {
        if (csr == null) {
            throw new CertificateAuthorityException("invalid_request", "CSR parsing failed", HttpStatus.BAD_REQUEST);
        }
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
        JcaContentVerifierProviderBuilder jcaContentVerifierProviderBuilder = new JcaContentVerifierProviderBuilder();
        if (!csr.isSignatureValid(jcaContentVerifierProviderBuilder.build(publicKey))) {
            throw new CertificateAuthorityException("invalid_request", "CSR signature is invalid", HttpStatus.BAD_REQUEST);
        }
    }

    protected void validateCertificateAuthority(CertificateAuthority certificateAuthority) throws Exception {
        if (certificateAuthority.getCertificate().getNotAfter().before(new Date())) {
            throw new CertificateAuthorityException("invalid_request", "Certificate authority is expired", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Sign a root CA certificate for certificate and CRL signing.  This certificate is self-signed.
     */
    @Deprecated
    protected X509Certificate signRootCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        return signLeafCertificate(
                certificateAuthority,
                csr,
                createCASubject(new X509CertificateHolder(certificateAuthority.getCertificate().getEncoded()).getSubject(), DIGDIR_ORGNO),
                List.of(
                        Extension.create(Extension.basicConstraints, true, new BasicConstraints(true)),
                        Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
                ));
    }

    /**
     * Sign an intermediate CA certificate for certificate and CRL signing.  This certificate is signed by the root CA
     * and references the root CA CRL,
     */
    @Deprecated
    protected X509Certificate signIntermediateCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        return signLeafCertificate(certificateAuthority,
                csr,
                createCASubject(new X509CertificateHolder(certificateAuthority.getCertificate().getEncoded()).getSubject(), DIGDIR_ORGNO),
                List.of(
                        Extension.create(Extension.basicConstraints, true, new BasicConstraints(true)),
                        createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()),
                        Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)),
                        createAuthorityInformationAccess(certificateAuthority.getCertificateUri())));
    }

    /**
     * Signs an end-entity certificate for an organization with an intermediate CA certificate.
     */
    public X509Certificate signLeafCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr, SubjectAttributes subjectAttributes) throws Exception {
        validateCSR(csr);
        List<Extension> extensions = new ArrayList<>();
        // basic + relation to CA
        extensions.add(Extension.create(Extension.basicConstraints, true, new BasicConstraints(false)));
        extensions.add(createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()));
        extensions.add(createAuthorityInformationAccess(certificateAuthority.getCertificateUri()));
        // key usage
        extensions.add(Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)));
        // add extensions from CA certificate profile
        addExtensionsFromCertificateProfile(certificateAuthority.getCertificateProfile(), extensions);
        // add extensions from CSR
        addSupportedExtensionsFromCsr(csr, extensions);
        X500Name subject = createLeafSubject(csr.getSubject(), subjectAttributes);
        X509Certificate certificate = signLeafCertificate(certificateAuthority, csr, subject, extensions);
        certificateRepository.save(new Certificate(certificate, certificateAuthority.getId()));
        return certificate;
    }

    /**
     * Adds known requested extensions from CSR.  Only SAN DNS is supported.
     *
     * @param csr
     * @param extensions
     */
    private void addSupportedExtensionsFromCsr(PKCS10CertificationRequest csr, List<Extension> extensions) {
        if (csr.getRequestedExtensions() == null) {
            return;
        }
        if (csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName) != null) {
            extensions.add(csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName));
        }
    }

    /**
     * Adds extensions from certificate authority's certificate profile.
     */
    private void addExtensionsFromCertificateProfile(CertificateProfile certificateProfile, List<Extension> extensions) throws Exception {
        if (certificateProfile == null) {
            return;
        }
        for (String oid : certificateProfile.getExtendedKeyUsage()) {
            extensions.add(Extension.create(
                    Extension.extendedKeyUsage,
                    true,
                    new ExtendedKeyUsage(KeyPurposeId.getInstance(new ASN1ObjectIdentifier(oid)))));
        }
        if (certificateProfile.getQcStatements() != null) {
            if (StringUtils.hasText(certificateProfile.getQcStatements().getQcType())) {
                extensions.add(
                        Extension.create(Extension.qCStatements,
                        true,
                        new QCStatement(
                                ETSIQCObjectIdentifiers.id_etsi_qcs_QcType,
                                new DERUTF8String(certificateProfile.getQcStatements().getQcType()))));
            }
        }
    }

    protected X509Certificate signLeafCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr, X500Name subject, List<Extension> extensions) throws Exception {
        validateCertificateAuthority(certificateAuthority);
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
                X500Name.getInstance(certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded()),
                generateSerialNumber(),
                new Date(System.currentTimeMillis()),
                calculateExpiryDate(certificateAuthority.getLifetimeDays(), certificateAuthority.getCertificate().getNotAfter()),
                subject,
                keyInfo)
                .addExtension(Extension.authorityKeyIdentifier, false,
                        new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(certificateAuthority.getPublicKey()))
                .addExtension(Extension.subjectKeyIdentifier, false,
                        new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));
        for (Extension extension : extensions) {
            x509v3CertificateBuilder.addExtension(extension);
        }
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNATURE_ALGORITHM_SHA_512_WITH_ECDSA);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter caPrivateKey = PrivateKeyFactory.createKey(certificateAuthority.getPrivateKey().getEncoded());
        ContentSigner sigGen = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateKey);
        X509CertificateHolder holder = x509v3CertificateBuilder.build(sigGen);
        org.bouncycastle.asn1.x509.Certificate eeX509CertificateStructure = holder.toASN1Structure();
        CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream is = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    protected BigInteger generateSerialNumber() {
        return SerialNumberUtils.generateSerialNumber();
    }

    protected X500Name createCASubject(X500Name csrSubjectName, String orgno) {
        X500NameBuilder x500NameBuilder = new X500NameBuilder();
        for (RDN rdn : csrSubjectName.getRDNs()) {
            x500NameBuilder.addMultiValuedRDN(rdn.getTypesAndValues());
        }
        x500NameBuilder.addRDN(BCStyle.C, "NO");
        x500NameBuilder.addRDN(BCStyle.O, "DIGITALISERINGSDIREKTORATET");
        x500NameBuilder.addRDN(ASN1ObjectIdentifier.tryFromID(OID_ORGANIZATION_NUMBER), "NTRNO-NOFOR.%s".formatted(orgno));
        return x500NameBuilder.build();
    }

    protected X500Name createLeafSubject(X500Name csrSubjectName, SubjectAttributes subjectAttributes) {
        X500NameBuilder x500NameBuilder = new X500NameBuilder();
        x500NameBuilder.addRDN(BCStyle.C, "NO");
        x500NameBuilder.addRDN(BCStyle.O, subjectAttributes.legalName().trim());
        x500NameBuilder.addRDN(BCStyle.CN, subjectAttributes.tradeName().trim());
        x500NameBuilder.addRDN(ASN1ObjectIdentifier.tryFromID(OID_ORGANIZATION_NUMBER), "NTRNO-NOFOR.%s".formatted(subjectAttributes.orgno().trim()));
        return x500NameBuilder.build();
    }

    protected Extension createCrlDistributionPointExtension(URI uri) throws IOException {
        GeneralNames generalNames = new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, uri.toString()));
        DistributionPointName distributionPointName = new DistributionPointName(generalNames);
        DistributionPoint distributionPoint = new DistributionPoint(distributionPointName, null, null);
        CRLDistPoint crlDistPoint = new CRLDistPoint(new DistributionPoint[]{distributionPoint});
        return Extension.create(Extension.cRLDistributionPoints, false, crlDistPoint);
    }

    protected Extension createAuthorityInformationAccess(URI uri) throws Exception {
        return Extension.create(Extension.authorityInfoAccess,
                false,
                new AuthorityInformationAccess(
                        new AccessDescription(
                                AccessDescription.id_ad_caIssuers,
                                new GeneralName(GeneralName.uniformResourceIdentifier,
                                        uri.toString()))));
    }

    protected Date calculateExpiryDate(long lifetimeDays, Date issuerExpiryDate) {
        Date expires = new Date(System.currentTimeMillis() + lifetimeDays * 24 * 60 * 60 * 1000);
        if (expires.after(issuerExpiryDate)) {
            return issuerExpiryDate;
        }
        return expires;
    }


    public void revokeCertificate(CertificateAuthority certificateAuthority, X509Certificate certificate, CRLReason reason) {
        revokeCertificate(certificateAuthority, SerialNumberUtils.convertToString(certificate.getSerialNumber()), reason.getValue().intValue());
    }

    public void revokeCertificate(CertificateAuthority certificateAuthority, String serialNumber, int reason) {
        Certificate issuedCertificate = certificateRepository.findByIssuerCaAndSerialNo(certificateAuthority.getId(), serialNumber);
        if (issuedCertificate == null) {
            throw new CertificateAuthorityException("invalid_request", "Cannot revoke unknown certificate", HttpStatus.BAD_REQUEST);
        }
        if (issuedCertificate.isRevoked()) {
            throw new CertificateAuthorityException("invalid_request", "Cannot revoke revoked certificate", HttpStatus.BAD_REQUEST);
        }
        issuedCertificate.revoke(reason);
        certificateRepository.saveAndFlush(issuedCertificate);
    }

    /**
     * Create and sign empty CRL for a certificate authority.
     */
    public X509CRL createCRL(CertificateAuthority certificateAuthority) throws Exception {
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                X500Name.getInstance(certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded()),
                new Date(System.currentTimeMillis()))
                .setNextUpdate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        List<Certificate> revokedCertificates = certificateRepository.findRevokedCertificates(certificateAuthority.getId());
        for (Certificate revokedCertificate : revokedCertificates) {
            crlBuilder.addCRLEntry(SerialNumberUtils.convertFromString(revokedCertificate.getSerialNo()), new Date(revokedCertificate.getRevokedAtMs()), revokedCertificate.getRevocationReason());
        }
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNATURE_ALGORITHM_SHA_512_WITH_ECDSA);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter caPrivateKey = PrivateKeyFactory.createKey(certificateAuthority.getPrivateKey().getEncoded());
        ContentSigner sigGen = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateKey);
        X509CRLHolder crlHolder = crlBuilder.build(sigGen);
        ASN1Object asn1Object = crlHolder.toASN1Structure();
        CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream is = new ByteArrayInputStream(asn1Object.getEncoded())) {
            return (X509CRL) cf.generateCRL(is);
        }
    }

    /**
     * Creates a CSR for a certificate authority.
     */
    protected PKCS10CertificationRequest createCSR(CertificateAuthority certificateAuthority) throws Exception {
        PKCS10CertificationRequestBuilder p10Builder =
                new JcaPKCS10CertificationRequestBuilder(
                        certificateAuthority.getCertificate().getSubjectX500Principal(),
                        certificateAuthority.getPublicKey());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM_SHA_512_WITH_ECDSA);
        ContentSigner signer = csBuilder.build(certificateAuthority.getPrivateKey());
        return p10Builder.build(signer);
    }

}
