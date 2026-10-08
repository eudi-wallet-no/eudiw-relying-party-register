package no.idporten.eudiw.credential.registry.response.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Claims (
        List<Object> path,
        List<Display> display
){
}
