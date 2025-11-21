package no.idporten.eudiw.rp.admin.security;

import lombok.Getter;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

public class SecurityTestUtils {

    public static SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor
    adminOidcLogin() {
        return oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public static SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor
    oidcLoginForOrgno(String orgno) {
        return oidcLogin().authorities(new ReporteeAuthority(orgno, "dummy-name", "https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer", true));
    }

    @Getter
    public static class ResultCaptor<T> implements Answer<T> {
        private T result = null;

        @Override
        @SuppressWarnings("unchecked")
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            result = (T) invocationOnMock.callRealMethod();
            return result;
        }
    }

}
