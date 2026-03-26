package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Service for XML signing.
 */
@Service
public class XMLSignerService {

    public static final String PROVIDER_XMLDSIG = "XMLDSig";

    static {
        System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    }

    public static final String NAMESPACE_XMLDSIG = "ds";
    public static final String ELEMENT_SIGNATURE = "Signature";
    public static final String MECHANISM_DOM = "DOM";

    private final Trustlist612Properties properties;
    private final KeystoreManager keystoreManager;


    @Autowired
    public XMLSignerService(KeystoreManager keystoreManager, Trustlist612Properties properties) {
        this.keystoreManager = keystoreManager;
        this.properties = properties;
    }

    private KeyProvider getKeystore(){
        return keystoreManager.getKeyProvider(properties.keystore());
    }

    private SignedInfo createSignedInfo(XMLSignatureFactory xmlSignatureFactory) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        CanonicalizationMethod c14nMethod = xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
        DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA512, null);
        SignatureMethod signMethod = xmlSignatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA512, null);
        List<Transform> transforms = List.of(
                xmlSignatureFactory.newTransform(Transform.ENVELOPED, (C14NMethodParameterSpec) null),
                xmlSignatureFactory.newTransform(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null)
        );
        Reference referenceDoc = xmlSignatureFactory.newReference("", digestMethod, transforms, null, null);
        List<Reference> references = Collections.singletonList(referenceDoc);
        return xmlSignatureFactory.newSignedInfo(c14nMethod, signMethod, references);
    }

    private KeyInfo createKeyInfo(XMLSignatureFactory xmlSignatureFactory) {
        Certificate certificate = getKeystore().certificate();
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(List.of(certificate));
        return keyInfoFactory.newKeyInfo(List.of(x509Data));
    }


    public Document createEnvelopedSignature(Document document) {
        PrivateKey privateKey = getKeystore().privateKey();
        XMLSignatureFactory xmlSignatureFactory;
        try {
            xmlSignatureFactory = XMLSignatureFactory.getInstance(MECHANISM_DOM, PROVIDER_XMLDSIG);
        } catch (NoSuchProviderException e) {
            throw new SigningException("Failed to get XMLSignatureFactory instance", e);
        }
        SignedInfo signedInfo;
        try {
            signedInfo = createSignedInfo(xmlSignatureFactory);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new SigningException("Setup of SignedInfo failed", e);
        }
        KeyInfo keyInfo = createKeyInfo(xmlSignatureFactory);
        XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, null, null, null);
        Element rootNode = document.getDocumentElement();
        DOMSignContext domSignContext = new DOMSignContext(privateKey, rootNode);
        domSignContext.setDefaultNamespacePrefix(NAMESPACE_XMLDSIG);
        try {
            xmlSignature.sign(domSignContext);
        } catch (MarshalException|XMLSignatureException e) {
            throw new SigningException("Failed to sign document", e);
        }
        return document;
    }

    public boolean validateEnvelopedSignature(Document document) {
        Node signatureNode = document.getElementsByTagNameNS(XMLSignature.XMLNS, ELEMENT_SIGNATURE).item(0);
        DOMValidateContext validateContext = new DOMValidateContext(KeySelector.singletonKeySelector(getKeystore().publicKey()), signatureNode);
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance(MECHANISM_DOM);
        XMLSignature signature;
        try {
            signature = factory.unmarshalXMLSignature(validateContext);
        } catch (MarshalException e) {
            throw new SigningException("Failed to unmarshal XMLSignature of document", e);
        }
        try {
            return signature.validate(validateContext);
        } catch (XMLSignatureException e) {
            throw new SigningException("Failed to validate signature of document", e);
        }
    }

}
