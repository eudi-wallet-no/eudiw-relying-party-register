package no.idporten.eudiw.rp.admin.security.ansattporten.authzdetails;

import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("junit")
public class RequestAuthorizationDetailsSerializationTest {

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @Test
    public void testRepresentationIsRequiredInAllSerializedRequestAuthorizationDetails() {

        JsonMapper jsonMapper = new JsonMapper();

        var requestAuthorizationDetails = ansattportenProperties.getRequestAuthorizationDetails();
        var entraIdRequestAuthorizationDetails = ansattportenProperties.getEntraIdRequestAuthorizationDetails();

        requestAuthorizationDetails.forEach(requestAuthzDetail -> {
            Map<String, String> serialized =
                jsonMapper.convertValue(requestAuthzDetail, new TypeReference<>() { });
            assertTrue(serialized.containsKey("representation_is_required"));
            assertEquals("true", serialized.get("representation_is_required"));
        });

        entraIdRequestAuthorizationDetails.forEach(requestAuthzDetail -> {
            Map<String, String> serialized =
                jsonMapper.convertValue(requestAuthzDetail, new TypeReference<>() { });
            assertTrue(serialized.containsKey("representation_is_required"));
            assertEquals("true", serialized.get("representation_is_required"));
        });
    }
}
