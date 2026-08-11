let panels = new Map(Array.from(document.getElementById("panels_container").children)
                          .map(panel => [panel.id, panel]));
let defaultPanelId = panels.keys().next().value;
let tabs = Array.from(document.getElementById("tabs_container").children);

function setTab(id) {
    id = panels.has(id) ? id : defaultPanelId;
    panels.forEach(panel =>
        panel.style.display = panel.id === id ? "block" : "none");
    tabs.values().forEach(tab => tab.ariaSelected = tab.hash === id);
}

window.onhashchange = () => setTab(location.hash);
window.onload = () => setTab(location.hash);
