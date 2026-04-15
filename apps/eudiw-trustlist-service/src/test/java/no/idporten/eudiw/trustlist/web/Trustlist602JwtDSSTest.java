package no.idporten.eudiw.trustlist.web;


import eu.europa.esig.dss.jades.DSSJsonUtils;
import eu.europa.esig.dss.jades.validation.JWS;
import eu.europa.esig.lote.json.LOTEJsonUtils;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.List;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DisplayName("When using 602 controllers")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class Trustlist602JwtDSSTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist602Properties properties;

    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("use DSS-tests to validate the JWS returned by the 602 controllers is correctly formed and valid according to JAdES schema, and that the payload is valid according to LOTE schema")
    void verifyListIsOnlyGeneratedOnceOnStartup(String trustlist) throws Exception {

        String uri = properties.tsl602().get(trustlist).path() + ".jws";
        String contentType = "application/jose+json";

        MvcResult mvcResult1 = mockMvc.perform(get(uri))
                .andExpect(content().contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();
        String jwsString = mvcResult1.getResponse().getContentAsString();
        assertNotNull(jwsString);

        String[] jwtParts = jwsString.split("\\.");
        assertEquals(3, jwtParts.length);
        dssJadesValidation(jwtParts);
        String json = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
        assertNotNull(json);

        if (!TSL_WALLET.equals(trustlist)) { // Trustlist is not valid without services, remove if-check when first wallet service is added
            validateDssJson(json);
        }

    }

    private static void validateDssJson(String jws) {
        List<String> errors = LOTEJsonUtils.getInstance().validateAgainstSchema(jws);
        //System.out.println("Decoded JSON: " + jws);
        assertEquals(0, errors.size(), "JSON should be valid according to LOTE schema. Errors: " + errors);
    }


    /**
     * DSS validation of the JWS according to JAdES schema, this will ensure that the JWS is correctly formed and can be validated by DSS library.
     */
    private static void dssJadesValidation(String[] jwtParts) {
        JWS jws = new JWS(jwtParts);
        List<String> errors = DSSJsonUtils.validateAgainstJAdESSchema(jws);
        assertEquals(0, errors.size(), "JWS should be valid according to JAdES schema. Errors: " + errors);
    }

}