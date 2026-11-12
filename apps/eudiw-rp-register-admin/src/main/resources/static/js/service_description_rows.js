const rows = document.getElementById("description-rows");
const rowTemplate = document.getElementById("description-row-template");

function reindexRows() {
    rows.querySelectorAll(".description-row").forEach((row, index) => {
        row.querySelectorAll("input, textarea").forEach(field => {
            field.name = field.name.replace(/descriptions\[\d+]/, `descriptions[${index}]`);
        });
    });
}

function removeRow(row) {
    if (rows.querySelectorAll(".description-row").length === 1) {
        row.querySelectorAll("input, textarea").forEach(field => field.value = "");
        row.querySelector(".description-lang").focus();
        return;
    }
    const focusTarget = row.nextElementSibling ?? row.previousElementSibling;
    row.remove();
    reindexRows();
    focusTarget.querySelector(".description-lang").focus();
}

function addRow() {
    rows.appendChild(rowTemplate.content.cloneNode(true));
    reindexRows();
    rows.lastElementChild.querySelector(".description-lang").focus();
}

rows.addEventListener("click", event => {
    const button = event.target.closest("[data-remove-description]");
    if (button) {
        removeRow(button.closest(".description-row"));
    }
});
document.getElementById("add-description").addEventListener("click", addRow);
