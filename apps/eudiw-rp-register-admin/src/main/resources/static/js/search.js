(() => {
    const form = document.getElementById("search_form");

    if (!form) {
        return;
    }

    const searchTerm = form.querySelector('input[name="searchTerm"]');
    const clearSearch = form.querySelector('button[type="reset"]');
    const filterToggle = document.getElementById("filter-toggle");
    const filterDialog = document.getElementById("filter-dialog");
    const filterCancel = document.getElementById("filter-cancel");
    const filters = form.querySelectorAll('input[type="checkbox"]');

    filterToggle.addEventListener("click", () => filterDialog.showModal());
    filterCancel.addEventListener("click", () => filterDialog.close());
    filterDialog.addEventListener("close", () => {
        filters.forEach(filter => {
            filter.checked = filter.defaultChecked;
        });
    });

    clearSearch.addEventListener("click", event => {
        event.preventDefault();
        searchTerm.value = "";
        searchTerm.focus();
    });

    document.querySelectorAll("[data-reset-filters]").forEach(button => {
        button.addEventListener("click", event => {
            event.preventDefault();
            filters.forEach(filter => {
                filter.checked = false;
            });

            if (button.dataset.resetFilters === "clear-search") {
                searchTerm.value = "";
            }

            const submitButton = filterDialog.open
                ? filterDialog.querySelector('button[type="submit"]')
                : form.querySelector('button[type="submit"]');
            form.requestSubmit(submitButton);
        });
    });

    form.addEventListener("submit", event => {
        const button = event.submitter || form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.setAttribute("aria-busy", "true");
        button.textContent = "Søkjer";
    });

    window.addEventListener("pageshow", () => {
        form.querySelectorAll('button[type="submit"]').forEach(button => {
            button.disabled = false;
            button.removeAttribute("aria-busy");
            button.textContent = "Søk";
        });
    });

    filterToggle.hidden = false;
    filterCancel.hidden = false;
    clearSearch.hidden = false;
})();
