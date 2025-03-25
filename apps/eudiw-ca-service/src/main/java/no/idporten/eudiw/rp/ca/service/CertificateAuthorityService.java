package no.idporten.eudiw.rp.ca.service;

import no.idporten.eudiw.rp.ca.config.CertificateAuthority;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.x500.X500Name;
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
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemWriter;
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

    public PKCS10CertificationRequest decodeCsr(String csr) throws Exception {
        return decodeFromPem(csr, PKCS10CertificationRequest.class);
    }

    protected void validateCSR(PKCS10CertificationRequest csr) throws Exception {
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
        JcaContentVerifierProviderBuilder jcaContentVerifierProviderBuilder = new JcaContentVerifierProviderBuilder();
        if (! csr.isSignatureValid(jcaContentVerifierProviderBuilder.build(publicKey))) {
            throw new RuntimeException("CSR signature is invalid");
        }
    }

    /**
     * Sign a root CA certificate for certificate and CRL signing.  This certificate is self-signed.
     */
    protected X509Certificate signRootCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        return signCertificate(certificateAuthority, csr, List.of(
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
        return signCertificate(certificateAuthority, csr, List.of(
                Extension.create(Extension.basicConstraints, true, new BasicConstraints(true)),
                createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()),
                Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
        ));
    }

    /**
     * Signs an end-entity certificate with an intermediate CA certificate.
     */
    public X509Certificate signCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr) throws Exception {
        validateCSR(csr);
        List<Extension> extensions = new ArrayList<>();
        extensions.add(Extension.create(Extension.basicConstraints, true, new BasicConstraints(false)));
        extensions.add(createCrlDistributionPointExtension(certificateAuthority.getCrlDistributionPoint()));
        extensions.add(Extension.create(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)));
        if (csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName) != null) {
            extensions.add(csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName));
        }
        return signCertificate(certificateAuthority, csr, extensions);
    }

    protected X509Certificate signCertificate(CertificateAuthority certificateAuthority, PKCS10CertificationRequest csr, List<Extension> extensions) throws Exception {
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
                X500Name.getInstance(certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded()),
                BigInteger.valueOf(new SecureRandom().nextInt()),
                new Date(System.currentTimeMillis()),
                calculateExpiryDate(certificateAuthority.getLifetimeDays(), certificateAuthority.getCertificate().getNotAfter()),
                csr.getSubject(),
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

    protected Extension createCrlDistributionPointExtension(URI uri) throws IOException {
        GeneralNames generalNames = new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, uri.toString()));
        DistributionPointName distributionPointName = new DistributionPointName(generalNames);
        DistributionPoint distributionPoint = new DistributionPoint(distributionPointName, null, null);
        CRLDistPoint crlDistPoint = new CRLDistPoint(new DistributionPoint[]{distributionPoint});
        return Extension.create(Extension.cRLDistributionPoints, false, crlDistPoint);
    }

    protected Date calculateExpiryDate(long lifetimeDays, Date isserExpiryDate) {
        Date expires = new Date(System.currentTimeMillis() + lifetimeDays * 24 * 60 * 60 * 1000);
        if (expires.after(isserExpiryDate)) {
            return isserExpiryDate;
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

    public String encodeToPem(Object o) {
        try (StringWriter stringWriter = new StringWriter(); PemWriter pemWriter = new PemWriter(stringWriter)) {
            JcaMiscPEMGenerator jcaMiscPEMGenerator = new JcaMiscPEMGenerator(o);
            pemWriter.writeObject(jcaMiscPEMGenerator);
            pemWriter.flush();
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode certificate to PEM", e);
        }
    }

    public <T> T decodeFromPem(String pem, Class<T> clazz) {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            return clazz.cast(pemParser.readObject());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode object from PEM", e);
        }
    }

}
