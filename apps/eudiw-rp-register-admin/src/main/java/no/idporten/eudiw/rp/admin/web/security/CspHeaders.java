package no.idporten.eudiw.rp.admin.web.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public final class CspHeaders {

    private CspHeaders() {
    }

    public static void configure(HttpSecurity http, CspProperties csp) {
        http.headers(headers -> headers.contentSecurityPolicy(policy -> {
            policy.policyDirectives(csp.policy());
            if (csp.mode() == CspProperties.Mode.REPORT_ONLY) {
                policy.reportOnly();
            }
        }));
    }
}
