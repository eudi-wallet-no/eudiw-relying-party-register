package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuerMetadataService {

    public static final String SD_JWT_FORMAT = "dc+sd-jwt";

    private final IssuerMetadataClient client;
    private final JsonMapper jsonMapper;

    public List<Definition> load(URI endpoint) throws IOException {
        String issuer = IssuerMetadataClient.issuerFor(endpoint);

        byte[] response = client.fetch(endpoint);
        JsonNode metadata;
        try {
            metadata = jsonMapper.readTree(response);
        } catch (JacksonException e) {
            throw new IssuerMetadataException("Invalid issuer metadata JSON", e);
        }

        if (metadata == null || !issuer.equals(text(metadata.path("credential_issuer")))) {
            throw new IssuerMetadataException("Unexpected credential issuer");
        }

        var configurations = metadata.path("credential_configurations_supported");
        if (!configurations.isObject() || configurations.isEmpty()) {
            throw new IssuerMetadataException("Missing or invalid credential configurations");
        }

        return configurations.properties().stream()
            .map(entry -> definition(entry.getKey(), entry.getValue()))
            .toList();
    }

    public record Definition(String id, String label, String vct, List<Claim> claims, String error) {
    }

    public record Claim(String label, List<Object> path) {
    }

    private static Definition definition(String id, JsonNode configuration) {
        String format = text(configuration.path("format"));
        var metadata = configuration.path("credential_metadata");
        String label = label(metadata, id);
        String vct = text(configuration.path("vct"));
        var claims = metadata.path("claims");

        if (!format.isEmpty() && !SD_JWT_FORMAT.equals(format)) {
            return new Definition(id, label, vct, List.of(), "Bevisformatet kan ikkje veljast enno.");
        }

        if (format.isEmpty() || vct.isEmpty() || !claims.isArray() || claims.isEmpty()) {
            return incomplete(id, label);
        }

        List<Claim> options = new ArrayList<>();
        for (JsonNode claim : claims) {
            var path = claim.path("path");

            if (!path.isArray() || path.isEmpty()) {
                return incomplete(id, label);
            }

            List<Object> parts = new ArrayList<>();
            for (JsonNode part : path) {
                if (part.isNull()) {
                    parts.add(null);
                } else if (part.isIntegralNumber() && part.bigIntegerValue().signum() >= 0) {
                    parts.add(part.bigIntegerValue());
                } else if (part.isString() && !text(part).isEmpty()) {
                    parts.add(part.stringValue());
                } else {
                    return incomplete(id, label);
                }
            }

            options.add(new Claim(label(claim, path.toString()), Collections.unmodifiableList(parts)));
        }

        return new Definition(id, label, vct, List.copyOf(options), null);
    }

    private static Definition incomplete(String id, String label) {
        return new Definition(id, label, "", List.of(), "Beviset manglar metadata som trengst for å velje attributt.");
    }

    private static String label(JsonNode node, String fallback) {
        for (JsonNode display : node.path("display")) {
            String name = text(display.path("name"));
            if (!name.isEmpty()) {
                return name;
            }
        }
        return fallback;
    }

    private static String text(JsonNode node) {
        return node.isString() && !node.stringValue().isBlank() ? node.stringValue() : "";
    }
}
