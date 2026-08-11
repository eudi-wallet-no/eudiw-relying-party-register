package no.idporten.eudiw.rp.admin.web.security.exception;

public class ErrorCodes {
    private ErrorCodes() { }

    public static final String INVALID_AUTHORIZATION_DETAILS = "invalid_authorization_details";
    public static final String ENTRA_ID_NOT_ALLOWED = "entra_id_not_allowed";
    public static final String INSUFFICIENT_AUTHORITY = "insufficient_authority";
    public static final String INVALID_TOKEN = "invalid_token";
}
