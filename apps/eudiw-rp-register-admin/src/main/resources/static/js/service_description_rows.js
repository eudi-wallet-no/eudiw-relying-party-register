const rows = document.getElementById("description-rows");
const rowTemplate = document.getElementById("description-row-template");

function reindexRows() {
    rows.querySelectorAll(".description-row").forEach((row, index) => {
        row.querySelectorAll("input").forEach(input => {
            input.name = input.name.replace(/descriptions\[\d+]/, `descriptions[${index}]`);
        });
    });
}

function removeRow(row) {
    if (rows.querySelectorAll(".description-row").length === 1) {
        row.querySelectorAll("input").forEach(input => input.value = "");
        row.querySelector(".description-lang").focus();
        return;
    }
    row.remove();
    reindexRows();
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
