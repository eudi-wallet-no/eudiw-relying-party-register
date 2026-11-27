const purposeRows = document.getElementById("purpose-rows");
const purposeRowTemplate = document.getElementById("purpose-row-template");

function reindexPurposeRows() {
    const rows = purposeRows.querySelectorAll(".intended-use-purpose-row");
    rows.forEach((row, index) => {
        row.querySelectorAll("input, textarea").forEach(field => {
            field.name = field.name.replace(/purpose\[\d+]/, `purpose[${index}]`);
            field.id = field.id.replace(/purpose-(lang|content)-\d+/, `purpose-$1-${index}`);
        });
        row.querySelectorAll("label[for]").forEach(label => {
            label.htmlFor = label.htmlFor.replace(/purpose-(lang|content)-\d+/, `purpose-$1-${index}`);
        });
        const removeButton = row.querySelector("[data-remove-purpose]");
        removeButton.hidden = rows.length === 1;
        removeButton.setAttribute("aria-label", `Fjern føremål på språk ${index + 1}`);
    });
}

function removePurposeRow(row) {
    if (purposeRows.querySelectorAll(".intended-use-purpose-row").length === 1) {
        row.querySelectorAll("input, textarea").forEach(field => field.value = "");
        row.querySelector("input").focus();
        return;
    }

    const focusTarget = row.nextElementSibling ?? row.previousElementSibling;
    row.remove();
    reindexPurposeRows();
    focusTarget.querySelector("input").focus();
}

purposeRows.addEventListener("click", event => {
    const button = event.target.closest("[data-remove-purpose]");
    if (button) {
        removePurposeRow(button.closest(".intended-use-purpose-row"));
    }
});

document.getElementById("add-purpose").addEventListener("click", () => {
    purposeRows.appendChild(purposeRowTemplate.content.cloneNode(true));
    reindexPurposeRows();
    purposeRows.lastElementChild.querySelector("input").focus();
});

const addedCredentials = document.getElementById("added-credentials");
const credentialsEmpty = document.getElementById("credentials-empty");
const credentialTemplate = document.getElementById("credential-row-template");
const addCredential = document.getElementById("add-credential");

function reindexCredentials() {
    addedCredentials.querySelectorAll("[data-credential]").forEach((row, index) => {
        row.querySelectorAll("[name]").forEach(field => {
            field.name = field.name.replace(/credentials\[\d+]/, `credentials[${index}]`);
        });
        row.querySelectorAll("[id]").forEach(element => {
            element.id = element.id.replace(/credential-(name|vct|claimPaths)(-help|-error)?-\d+/g,
                `credential-$1$2-${index}`);
        });
        row.querySelectorAll("label[for]").forEach(label => {
            label.htmlFor = label.htmlFor.replace(/credential-(name|vct|claimPaths)-\d+/g,
                `credential-$1-${index}`);
        });
        row.querySelectorAll("[aria-describedby]").forEach(field => {
            field.setAttribute("aria-describedby", field.getAttribute("aria-describedby")
                .replace(/credential-(name|vct|claimPaths)(-help|-error)-\d+/g,
                    `credential-$1$2-${index}`));
        });
        row.querySelector("[data-remove-credential]").setAttribute("aria-label", `Fjern bevis ${index + 1}`);
    });
    credentialsEmpty.hidden = addedCredentials.childElementCount > 0;
}

addCredential.addEventListener("click", () => {
    const row = credentialTemplate.content.cloneNode(true);
    row.querySelectorAll(".ds-validation-message").forEach(error => error.remove());
    row.querySelectorAll("input, textarea").forEach(field => {
        field.value = "";
        field.removeAttribute("aria-invalid");
        field.removeAttribute("aria-describedby");
    });
    row.querySelector("textarea").setAttribute("aria-describedby", "credential-claimPaths-help-0");
    addedCredentials.appendChild(row);
    reindexCredentials();
    addedCredentials.lastElementChild.querySelector("input").focus();
});

addedCredentials.addEventListener("click", event => {
    const button = event.target.closest("[data-remove-credential]");
    if (button) {
        const row = button.closest("[data-credential]");
        const rows = [...addedCredentials.querySelectorAll("[data-credential]")];
        const removedIndex = rows.indexOf(row);
        const focusTarget = row.nextElementSibling ?? row.previousElementSibling;
        row.remove();
        document.querySelectorAll("[data-error-field]").forEach(link => {
            const match = link.dataset.errorField.match(/^credentials\[(\d+)]\.(.+)$/);
            if (!match) return;
            const index = Number(match[1]);
            if (index === removedIndex) {
                link.closest("li").remove();
            } else if (index > removedIndex) {
                link.dataset.errorField = `credentials[${index - 1}].${match[2]}`;
            }
        });
        const errorSummary = document.querySelector("[aria-labelledby='intended-use-errors-heading']");
        if (errorSummary && !errorSummary.querySelector("li")) errorSummary.remove();
        reindexCredentials();
        updateCredentialErrorLinks();
        (focusTarget?.querySelector("input") ?? addCredential).focus();
    }
});

function updateCredentialErrorLinks() {
    document.querySelectorAll("[data-error-field]").forEach(link => {
        const match = link.dataset.errorField.match(/^credentials\[(\d+)]\.(.+)$/);
        if (match) link.href = `#credential-${match[2]}-${match[1]}`;
    });
}

reindexCredentials();
updateCredentialErrorLinks();
