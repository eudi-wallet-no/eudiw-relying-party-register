const entitlement_container = document.getElementById("entitlement_container");
const eaa_container = document.getElementById("eaa_container");

const entitlement_input_field = document.getElementById("entitlement_field");
const eaa_namespace_input_field = document.getElementById("eaa_namespace_field");
const eaa_intent_input_field = document.getElementById("eaa_intent_field");

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

function makeListItem(content) {
    const elem = document.createElement('li');
    elem.innerHTML = content;

    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.innerText = "Slet";
    deleteButton.onclick = () => elem.remove();
    elem.appendChild(deleteButton);

    return elem;
}

function addEntitlementOnclick() {
    const entitlement = entitlement_input_field.value.trim();
    if (isSaneStringInput(entitlement)) {
        addEntitlement(entitlement);
        entitlement_input_field.value = "";
    }
}
function addEaaOnclick() {
    const namespace = eaa_namespace_input_field.value.trim();
    const intent = eaa_intent_input_field.value.trim();
    if (isSaneStringInput(namespace) && isSaneStringInput(intent)) {
        addEaa(namespace, intent);
        eaa_namespace_input_field.value = "";
        eaa_intent_input_field.value = "";
    }
}

function addEntitlement(entitlement) {
    const entitlementItem = makeListItem(
        `<div>${entitlement}
             <input type="hidden" name="entitlements" value="${entitlement}"></div>`);
    entitlement_container.appendChild(entitlementItem);
}
function addEaa(namespace, intent) {
    const idx = idxGen.next().value;
    const eaaItem = makeListItem(
        `<div>Namespace: ${namespace}
             <input type="hidden" name="eaas[${idx}].namespace" value="${namespace}"></div>
             <div>Intent: ${intent}
             <input type="hidden" name="eaas[${idx}].intent" value="${intent}"></div>`);
    eaa_container.appendChild(eaaItem);
}
