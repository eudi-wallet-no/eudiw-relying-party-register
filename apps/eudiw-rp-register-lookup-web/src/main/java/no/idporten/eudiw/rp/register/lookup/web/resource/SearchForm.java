package no.idporten.eudiw.rp.register.lookup.web.resource;

public record SearchForm(
    String orgno,
    SearchSector searchSector,
    boolean includeInactive
) {
    public enum SearchSector {
        PUBLIC,
        PRIVATE,
        ANY;
        private Boolean toDataBoolean() {
            return this == ANY ? null : this == PUBLIC;
        }
    }

    public static SearchForm empty() {
        return new SearchForm(null, SearchSector.ANY, false);
    }

    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource(
            this.orgno,
            this.searchSector.toDataBoolean(),
            this.includeInactive);
    }
}
