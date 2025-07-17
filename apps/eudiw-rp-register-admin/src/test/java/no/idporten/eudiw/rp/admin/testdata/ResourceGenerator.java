package no.idporten.eudiw.rp.admin.testdata;

import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;

public class ResourceGenerator extends TestDataGenerator {

    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources() {
        List<RelyingPartyEntitlement> entitlements = Arrays.asList(RelyingPartyEntitlement.values());
        Collections.shuffle(entitlements);
        return entitlements.subList(0, rng.nextInt(1, 4))
                           .stream()
                           .map(RelyingPartyEntitlement::toResource)
                           .toList();
    }

    public static RelyingPartyEaaResource generateRelyingPartyEaaResource() {
        return new RelyingPartyEaaResource(
            generateRandomString(5, 20),
            generateRandomString(5, 20)
        );
    }

    public static RelyingPartyResource generateRelyingPartyResource() {
        long timeNow = Instant.now().toEpochMilli();

        return new RelyingPartyResource(
            UUID.randomUUID(),
            generateValidOrgno(),
            generateName(),
            generateBoolean(),
            sampleRelyingPartyEntitlementResources(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            timeNow,
            timeNow,
            true
        );
    }
    public static RelyingPartiesResource generateRelyingPartiesResource() {
        return new RelyingPartiesResource(
            generateListBy(ResourceGenerator::generateRelyingPartyResource));
    }

    public static SearchForm generateSearchForm() {
        return new SearchForm(generateName(), generateBoolean(), new ArrayList<>());
    }

    public static RelyingPartyAccessCertificateResource generateCertificateResource()
        throws Exception {
        return new RelyingPartyAccessCertificateResource(
            CertificatesGenerator.generateX509Certificate());
    }

    public static RelyingPartyAccessCertificatesResource generateCertificatesResource()
        throws Exception {
        return generateCertificatesResource(rng.nextInt(0, 2));
    }
    public static RelyingPartyAccessCertificatesResource generateCertificatesResource(int n)
        throws Exception {
        List<X509Certificate> certs = new ArrayList<>();
        for (int i = 0; i < n; i++)
            certs.add(CertificatesGenerator.generateX509Certificate());

        return new RelyingPartyAccessCertificatesResource(
            certs.stream().map(RelyingPartyAccessCertificateResource::new).toList());
    }

    public static RelyingPartyCsrResource generateCsrResource() throws Exception {
       return new RelyingPartyCsrResource(CertificatesGenerator.generatePKCS10Csr());
    }

}
