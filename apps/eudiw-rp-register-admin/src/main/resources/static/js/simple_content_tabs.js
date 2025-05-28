let tabs = Array.from(document.getElementById("content_tabs").children);
let tab_ids = tabs.map(tab => tab.id);
function setTab(next_tab_id) {
    let next_tab = document.getElementById(next_tab_id);
    if (tabs.includes(next_tab)) {
        tabs.forEach(tab => tab.style.display = "none");
        next_tab.style.display = "block";
    }
}
window.onhashchange = () => setTab(location.hash);
window.onload = () => setTab(tab_ids.includes(location.hash) ? location.hash : tab_ids[0]);
