(() => {
    function showSpinner(button, label) {
        button.disabled = true;
        button.setAttribute("aria-busy", "true");
        button.setAttribute("aria-label", label);
        button.replaceChildren();

        const spinner = document.createElement("span");
        spinner.className = "ds-spinner";
        spinner.dataset.size = "sm";
        spinner.setAttribute("aria-hidden", "true");

        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("viewBox", "0 0 50 50");
        for (const className of ["ds-spinner__background", "ds-spinner__circle"]) {
            const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
            circle.setAttribute("class", className);
            circle.setAttribute("cx", "25");
            circle.setAttribute("cy", "25");
            circle.setAttribute("r", "20");
            circle.setAttribute("fill", "none");
            circle.setAttribute("stroke-width", "4");
            svg.append(circle);
        }
        spinner.append(svg);
        button.append(spinner);
    }

    document.querySelectorAll("#search_form, #index-search-form").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const button = event.submitter || form.querySelector('button[type="submit"]');
            if (button) showSpinner(button, "Søkjer");
        });
    });

    const searchForm = document.getElementById("search_form");
    if (searchForm) {
        const searchTerm = searchForm.querySelector("#search_term");
        const clearButton = searchForm.querySelector('.ds-search button[type="reset"]');
        clearButton?.addEventListener("click", (event) => {
            event.preventDefault();
            searchTerm.value = "";
            searchTerm.focus();
        });

        searchForm.querySelectorAll("[data-dialog-action]").forEach((button) => {
            button.addEventListener("click", () => {
                const dialog = document.getElementById(button.dataset.dialogTarget);
                if (!dialog) return;
                if (button.dataset.dialogAction === "show-modal" && !dialog.open) {
                    dialog.showModal();
                } else if (button.dataset.dialogAction === "close" && dialog.open) {
                    dialog.close();
                }
            });
        });

        const resetFilter = (button, clearSearchTerm) => {
            searchForm.querySelectorAll('input[type="checkbox"]').forEach((checkbox) => {
                checkbox.checked = false;
            });
            if (clearSearchTerm) {
                const input = searchForm.querySelector('input[name="searchTerm"]');
                if (input) input.value = "";
            }
            showSpinner(button, "Tilbakestillar");
            searchForm.requestSubmit();
        };

        document.getElementById("filter-reset")?.addEventListener("click", (event) => {
            resetFilter(event.currentTarget, false);
        });
        document.getElementById("filter-reset-all")?.addEventListener("click", (event) => {
            resetFilter(event.currentTarget, true);
        });
    }

    document.querySelectorAll("tr[data-clickdelegatefor]").forEach((row) => {
        const activateLink = () => {
            const link = row.querySelector("a[id]");
            link?.click();
        };
        row.addEventListener("click", (event) => {
            if (event.target.closest("a, button, input, select, textarea")) return;
            activateLink();
        });
        row.addEventListener("keydown", (event) => {
            if (event.target === row && (event.key === "Enter" || event.key === " ")) {
                event.preventDefault();
                activateLink();
            }
        });
    });

    document.querySelectorAll("#index-search-form .index-search__clear").forEach((button) => {
        button.addEventListener("click", () => {
            const form = button.closest("form");
            const input = form?.querySelector('input[name="searchTerm"]');
            if (!form || !input) return;
            input.value = "";
            form.requestSubmit();
        });
    });
})();