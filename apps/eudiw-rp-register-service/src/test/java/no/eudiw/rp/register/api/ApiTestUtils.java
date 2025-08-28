package no.eudiw.rp.register.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.RelyingPartiesResource;
import no.eudiw.rp.register.api.resource.RelyingPartyResource;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class ApiTestUtils {

    public static RelyingPartyResource toRelyingPartyResource(ResultActions result) throws Exception {
        return readResultActions(result, RelyingPartyResource.class);
    }
    public static RelyingPartiesResource toRelyingPartiesResource(ResultActions result) throws Exception {
        return readResultActions(result, RelyingPartiesResource.class);
    }

    public static <T> T readResultActions(ResultActions result, Class<T> klass)
        throws UnsupportedEncodingException, JsonProcessingException {

        String json = result.andReturn().getResponse().getContentAsString();
        return readJson(json, klass);
    }

    public static <T> T readJson(Object json, Class<T> klass) throws JsonProcessingException {
        return new ObjectMapper().readValue((String) json, klass);
    }

    public record PagedResponse<T>(List<T> content, PageMetadata page) {
        public record PageMetadata(long size, long number, long totalElements, long totalPages) {}
    }

    public static <T> PagedResponse<T> toPage(ResultActions result, Class<T> itemClass) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String json = result.andReturn().getResponse().getContentAsString();

        JavaType type = mapper.getTypeFactory()
            .constructParametricType(PagedResponse.class, itemClass);

        return mapper.readValue(json, type);
    }
}
