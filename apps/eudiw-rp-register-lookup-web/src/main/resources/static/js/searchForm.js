(() => {
    const form = document.getElementById("index-search-form");
    if (!form) return;
    form.addEventListener("submit", (e) => {
        const btn = e.submitter || form.querySelector("button[type=submit]");
        if (!btn) return;
        btn.disabled = true;
        btn.setAttribute("aria-busy", "true");
        btn.setAttribute("aria-label", "Søkjer");
        btn.innerHTML = '<span class="ds-spinner" data-size="sm" aria-hidden="true">'
            + '<svg viewBox="0 0 50 50">'
            + '<circle class="ds-spinner__background" cx="25" cy="25" r="20" fill="none" stroke-width="4"></circle>'
            + '<circle class="ds-spinner__circle" cx="25" cy="25" r="20" fill="none" stroke-width="4"></circle>'
            + '</svg></span>';
    });
})();