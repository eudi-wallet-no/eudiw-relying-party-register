package no.idporten.eudiw.rp.admin.web.utils;

import no.idporten.eudiw.rp.admin.web.form.BaseCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminCreateRelyingPartyForm;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

public class WebTestUtils {

    public static MockHttpServletRequestBuilder withEntitlementFormFields(
        MockHttpServletRequestBuilder builder,
        List<RelyingPartyEntitlementFormField> entitlementFormFields) {
        int index = 0;
        for (var entitlement : entitlementFormFields) {
            builder = builder.param("entitlements[%s].entitlement".formatted(index),
                                    entitlement.getEntitlement());
            builder = builder.param("entitlements[%s].credentialIssuerUrl".formatted(index),
                                    entitlement.getCredentialIssuerUrl());
            index++;
        }
        return builder;
    }

    public static MockHttpServletRequestBuilder withCreateForm(
        MockHttpServletRequestBuilder builder,
        BaseCreateRelyingPartyForm createForm) {

        builder = builder.param("tradeName", createForm.getTradeName());

        return builder;
    }

    public static MockHttpServletRequestBuilder withCreateForm(
        MockHttpServletRequestBuilder builder,
        AdminCreateRelyingPartyForm createForm) {

        builder = builder.param("tradeName", createForm.getTradeName());
        builder = builder.param("orgno", createForm.getOrgno());
        builder = withEntitlementFormFields(builder, createForm.getEntitlements());

        return builder;
    }

}
