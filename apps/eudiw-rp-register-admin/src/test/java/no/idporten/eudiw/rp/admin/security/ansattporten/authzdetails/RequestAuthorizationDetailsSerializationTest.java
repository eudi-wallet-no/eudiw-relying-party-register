package no.idporten.eudiw.rp.admin.security.ansattporten.authzdetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("local-security-test")
public class RequestAuthorizationDetailsSerializationTest {

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @Test
    public void testRepresentationIsRequiredInAllSerializedRequestAuthorizationDetails() {

        ObjectMapper objectMapper = new ObjectMapper();

        var requestAuthorizationDetails = ansattportenProperties.getRequestAuthorizationDetails();
        var entraIdRequestAuthorizationDetails = ansattportenProperties.getEntraIdRequestAuthorizationDetails();

        requestAuthorizationDetails.forEach(requestAuthzDetail -> {
            Map<String, String> serialized =
                objectMapper.convertValue(requestAuthzDetail, new TypeReference<>() { });
            assertTrue(serialized.containsKey("representation_is_required"));
            assertEquals("true", serialized.get("representation_is_required"));
        });

        entraIdRequestAuthorizationDetails.forEach(requestAuthzDetail -> {
            Map<String, String> serialized =
                objectMapper.convertValue(requestAuthzDetail, new TypeReference<>() { });
            assertTrue(serialized.containsKey("representation_is_required"));
            assertEquals("true", serialized.get("representation_is_required"));
        });
    }
}
