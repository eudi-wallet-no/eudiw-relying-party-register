package no.idporten.eudiw.trustlist.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.trustlist.config.KeyProvider;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Service for XML signing.
 */
@RequiredArgsConstructor
@Service
public class XMLSignerService {

    public static final String PROVIDER_XMLDSIG = "XMLDSig";

    static {
        System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    }

    public static final String NAMESPACE_XMLDSIG = "ds";
    public static final String ELEMENT_SIGNATURE = "Signature";
    public static final String MECHANISM_DOM = "DOM";

    private final KeyProvider tslKeyProvider;

    private SignedInfo createSignedInfo(XMLSignatureFactory xmlSignatureFactory) throws Exception {
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
        Certificate certificate = tslKeyProvider.getCertificate();
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(List.of(certificate));
        return keyInfoFactory.newKeyInfo(List.of(x509Data));
    }


    public Document createEnvelopedSignature(Document document) throws Exception {
        PrivateKey privateKey = tslKeyProvider.getPrivateKey();
        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance(MECHANISM_DOM, PROVIDER_XMLDSIG);
        SignedInfo signedInfo = createSignedInfo(xmlSignatureFactory);
        KeyInfo keyInfo = createKeyInfo(xmlSignatureFactory);
        XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, null, null, null);
        Element rootNode = document.getDocumentElement();
        DOMSignContext domSignContext = new DOMSignContext(privateKey, rootNode);
        domSignContext.setDefaultNamespacePrefix(NAMESPACE_XMLDSIG);
        xmlSignature.sign(domSignContext);
        return document;
    }

    public boolean validateEnvelopedSignature(Document document) throws Exception {
        Node signatureNode = document.getElementsByTagNameNS(XMLSignature.XMLNS, ELEMENT_SIGNATURE).item(0);
        DOMValidateContext validateContext = new DOMValidateContext(KeySelector.singletonKeySelector(tslKeyProvider.getPublicKey()), signatureNode);
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance(MECHANISM_DOM);
        XMLSignature signature = factory.unmarshalXMLSignature(validateContext);
        return signature.validate(validateContext);
    }

}
