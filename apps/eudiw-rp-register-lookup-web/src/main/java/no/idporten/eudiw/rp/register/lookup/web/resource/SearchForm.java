package no.idporten.eudiw.rp.register.lookup.web.resource;

public record SearchForm(
    String orgno,
    String name,
    SearchSector searchSector,
    boolean includeInactive
) {
    public enum SearchSector {
        PUBLIC  ("offentlig"),
        PRIVATE ("privat"),
        ANY     ("begge");
        public final String displayName;

        SearchSector(String displayName) {
            this.displayName = displayName;
        }
        private Boolean toDataBoolean() {
            return this == ANY ? null : this == PUBLIC;
        }
    }

    public static SearchForm empty() {
        return new SearchForm(null, null, SearchSector.ANY, false);
    }
    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource(
            this.orgno,
            this.name,
            this.searchSector.toDataBoolean(),
            this.includeInactive);
    }
    public SearchSector[] getSearchSectorOptions() {
        return SearchSector.values();
    }
}
