package no.idporten.eudiw.ca.service;

import no.idporten.eudiw.ca.config.CertificateAuthority;
import no.idporten.eudiw.ca.exception.CertificateAuthorityException;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CertificateAuthorityService {

    public static final String DIGDIR_ORGNO = "991825827";

    public PKCS10CertificationRequest decodeCsr(String csr) throws Exception {
        return decodeFromPem(csr, PKCS10CertificationRequest.class);
    }

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
    protected X509Certificate signRootCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        return signCertificate(
                certificateAuthority,
                csr,
                createSubjectWithOrgno(new X509CertificateHolder(certificateAuthority.getCertificate().getEncoded()).getSubject(), DIGDIR_ORGNO),
                List.of(
                        Extension.create(Extension.basicConstraints, true, new BasicConstraints(true)),
                        Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
                ));
    }

    /**
     * Sign an intermediate CA certificate for certificate and CRL signing.  This certificate is signed by the root CA
     * and references the root CA CRL,
     */
    protected X509Certificate signIntermediateCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        return signCertificate(certificateAuthority,
                csr,
                createSubjectWithOrgno(csr.getSubject(), DIGDIR_ORGNO),
                List.of(
                        Extension.create(Extension.basicConstraints, true, new BasicConstraints(true)),
                        createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()),
                        Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)),
                        createAuthorityInformationAccess(certificateAuthority.getCertificateUri())));
    }

    /**
     * Signs an end-entity certificate for an organization with an intermediate CA certificate.
     */
    public X509Certificate signCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr, String orgno) throws Exception {
        validateCSR(csr);
        List<Extension> extensions = new ArrayList<>();
        extensions.add(Extension.create(Extension.basicConstraints, true, new BasicConstraints(false)));
        extensions.add(createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()));
        extensions.add(Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)));
        extensions.add(createAuthorityInformationAccess(certificateAuthority.getCertificateUri()));
        if (csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName) != null) {
            extensions.add(csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName));
        }
        X500Name subject = createSubjectWithOrgno(csr.getSubject(), orgno);
        return signCertificate(certificateAuthority, csr, subject, extensions);
    }

    protected X509Certificate signCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr, X500Name subject, List<Extension> extensions) throws Exception {
        validateCertificateAuthority(certificateAuthority);
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
                X500Name.getInstance(certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded()),
                new BigInteger(64, new SecureRandom()),
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
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter caPrivateKey = PrivateKeyFactory.createKey(certificateAuthority.getPrivateKey().getEncoded());
        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateKey);
        X509CertificateHolder holder = x509v3CertificateBuilder.build(sigGen);
        org.bouncycastle.asn1.x509.Certificate eeX509CertificateStructure = holder.toASN1Structure();
        CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream is = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    protected X500Name createSubjectWithOrgno(X500Name requestedName, String orgno) {
        X500NameBuilder x500NameBuilder = new X500NameBuilder();
        for (RDN rdn : requestedName.getRDNs()) {
            x500NameBuilder.addMultiValuedRDN(rdn.getTypesAndValues());
        }
        return x500NameBuilder.addRDN(ASN1ObjectIdentifier.tryFromID("2.5.4.97"), "NTRNO-%s".formatted(orgno)).build();
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

    /**
     * Create and sign empty CRL for a certificate authority.
     */
    public X509CRL createCRL(CertificateAuthority certificateAuthority) throws Exception {
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                X500Name.getInstance(certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded()),
                new Date(System.currentTimeMillis()))
                .setNextUpdate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter caPrivateKey = PrivateKeyFactory.createKey(certificateAuthority.getPrivateKey().getEncoded());
        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateKey);
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
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
        ContentSigner signer = csBuilder.build(certificateAuthority.getPrivateKey());
        return p10Builder.build(signer);
    }

    public String encodeToPem(Object o) {
        try (StringWriter stringWriter = new StringWriter(); PemWriter pemWriter = new PemWriter(stringWriter)) {
            JcaMiscPEMGenerator jcaMiscPEMGenerator = new JcaMiscPEMGenerator(o);
            pemWriter.writeObject(jcaMiscPEMGenerator);
            pemWriter.flush();
            return stringWriter.toString();
        } catch (Exception e) {
            throw new CertificateAuthorityException("server_error", "Failed to encode DER to PEM", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public <T> T decodeFromPem(String pem, Class<T> clazz) {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            return clazz.cast(pemParser.readObject());
        } catch (Exception e) {
            throw new CertificateAuthorityException("invalid_request", "Failed to decode object from PEM", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public X509Certificate toX509Certificate(X509CertificateHolder certificateHolder) throws Exception {
        org.bouncycastle.asn1.x509.Certificate eeX509CertificateStructure = certificateHolder.toASN1Structure();
        CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream is = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

}
