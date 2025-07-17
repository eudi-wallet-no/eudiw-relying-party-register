const eaa_container = document.getElementById("eaa_container");

const eaa_namespace_input_field = document.getElementById("eaa_namespace_field");
const eaa_intent_input_field = document.getElementById("eaa_intent_field");

const eaa_namespace_error_msg = document.getElementById("eaa_namespace_error_msg");
const eaa_intent_error_msg = document.getElementById("eaa_intent_error_msg");

const showElem = elem => elem.style.display = "block";
const hideElem = elem => elem.style.display = "none";

eaa_namespace_input_field.oninput = () => hideElem(eaa_namespace_error_msg);
eaa_intent_input_field.oninput = () => hideElem(eaa_intent_error_msg);

function isSaneStringInput(str) {
    const allowed_chars_regex = /^[a-zA-ZæøåÆØÅ0-9.,\-:'"&/ ]+$/;
    return str.length <= 255 && allowed_chars_regex.test(str);
}
function* idxGenerator() {
    let i = 0;
    while (true)
        yield i++;
}
const idxGen = idxGenerator();

function makeDeleteButtonFor(elem) {
    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.innerText = "Slett";
    deleteButton.onclick = () => elem.remove();
    deleteButton.className = "ds-button";
    deleteButton.dataset.variant = "secondary";
    deleteButton.dataset.color = "danger";
    return deleteButton;
}

function makeTableItem(content) {
    const row = document.createElement("tr");
    row.innerHTML = content;
    const deleteCell = document.createElement("td");
    deleteCell.appendChild(makeDeleteButtonFor(row));
    row.appendChild(deleteCell);
    return row;
}

function addEaaOnclick() {
    const namespace = eaa_namespace_input_field.value.trim();
    const intent = eaa_intent_input_field.value.trim();
    const namespaceOk = isSaneStringInput(namespace);
    const intentOk = isSaneStringInput(intent);
    if (namespaceOk && intentOk) {
        addEaa(namespace, intent);
        eaa_namespace_input_field.value = "";
        eaa_intent_input_field.value = "";
    }
    if (!namespaceOk)
        showElem(eaa_namespace_error_msg);
    if (!intentOk)
        showElem(eaa_intent_error_msg);
}

function addEaa(namespace, intent) {
    if (namespace && intent) {
        const idx = idxGen.next().value;
        const eaaItem = makeTableItem(
            `<td>${namespace} <input type="hidden" name="eaas[${idx}].namespace" value="${namespace}"></td>
             <td>${intent} <input type="hidden" name="eaas[${idx}].intent" value="${intent}"></td>`
        );
        eaa_container.appendChild(eaaItem);
    }
}
