package no.idporten.eudiw.rp.admin.service.exception;

import lombok.Getter;

@Getter
public class RelyingPartyNotFoundException extends NotFoundException {

    private final String requested;
    public RelyingPartyNotFoundException(String errorDescription, String requested) {
        super(errorDescription);
        this.requested = requested;
    }
}
