let panels = new Map(Array.from(document.getElementById("panels_container").children)
                          .map(panel => [panel.id, panel]));
let defaultPanelId = panels.keys().next().value;
let tabs = Array.from(document.getElementById("tabs_container").children);

function setTab(hash) {
    const requestedPanelId = hash.slice(1);
    const panelId = panels.has(requestedPanelId) ? requestedPanelId : defaultPanelId;
    panels.forEach(panel => panel.classList.toggle("display-none", panel.id !== panelId));
    tabs.forEach(tab => {
        if (tab.hash === `#${panelId}`) {
            tab.setAttribute("aria-current", "location");
        } else {
            tab.removeAttribute("aria-current");
        }
    });
}

window.onhashchange = () => setTab(location.hash);
window.onload = () => setTab(location.hash);
