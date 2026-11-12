package no.idporten.eudiw.rp.admin.web.utils;

import no.idporten.eudiw.rp.admin.web.form.BaseCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
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

    public static MockHttpServletRequestBuilder withEaaFormFields(
        MockHttpServletRequestBuilder builder,
        List<RelyingPartyEaaFormField> eaaFormFields) {
        int index = 0;
        for (var eaa : eaaFormFields) {
            builder = builder.param("eaas[%s].namespace".formatted(index), eaa.getNamespace())
                             .param("eaas[%s].intent".formatted(index), eaa.getIntent());
            index++;
        }
        return builder;
    }

    public static MockHttpServletRequestBuilder withCreateForm(
        MockHttpServletRequestBuilder builder,
        BaseCreateRelyingPartyForm createForm) {

        builder = builder.param("tradeName", createForm.getTradeName());
        builder = withEaaFormFields(builder, createForm.getEaas());

        return builder;
    }

    public static MockHttpServletRequestBuilder withCreateForm(
        MockHttpServletRequestBuilder builder,
        AdminCreateRelyingPartyForm createForm) {

        builder = builder.param("tradeName", createForm.getTradeName());
        builder = builder.param("orgno", createForm.getOrgno());
        builder = withEaaFormFields(builder, createForm.getEaas());
        builder = withEntitlementFormFields(builder, createForm.getEntitlements());

        return builder;
    }

}
