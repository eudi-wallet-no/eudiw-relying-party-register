package no.eudiw.rp.register.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.RelyingPartiesResource;
import no.eudiw.rp.register.api.resource.RelyingPartyResource;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;

public class ApiTestUtils {

    public static RelyingPartyResource toRelyingPartyResource(ResultActions result) throws Exception {
        return readResultActions(result, RelyingPartyResource.class);
    }
    public static RelyingPartiesResource toRelyingPartiesResource(ResultActions result) throws Exception {
        return readResultActions(result, RelyingPartiesResource.class);
    }

    private static <T> T readResultActions(ResultActions result, Class<T> klass)
        throws UnsupportedEncodingException, JsonProcessingException {

        String json = result.andReturn().getResponse().getContentAsString();
        return readJson(json, klass);
    }

    public static <T> T readJson(Object json, Class<T> klass) throws JsonProcessingException {
        return new ObjectMapper().readValue((String) json, klass);
    }
}
