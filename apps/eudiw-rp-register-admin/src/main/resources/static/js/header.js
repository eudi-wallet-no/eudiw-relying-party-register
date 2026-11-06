(() => {
    const toggle = document.getElementById("header-nav-toggle");
    const toggleLabel = document.getElementById("header-nav-toggle-label");
    const navigation = document.getElementById("header-navigation");

    if (!toggle || !toggleLabel || !navigation) {
        return;
    }

    const mobile = window.matchMedia("(max-width: 767px)");

    function setNavigationOpen(open) {
        toggle.setAttribute("aria-expanded", String(open));
        toggle.setAttribute("aria-label", open ? "Lukk meny" : "Meny");
        toggleLabel.textContent = open ? "Lukk" : "Meny";
    }

    toggle.addEventListener("click", () => {
        setNavigationOpen(toggle.getAttribute("aria-expanded") !== "true");
    });

    document.addEventListener("keydown", event => {
        if (event.key !== "Escape" || !mobile.matches || toggle.getAttribute("aria-expanded") !== "true") {
            return;
        }

        if (document.activeElement !== toggle && !navigation.contains(document.activeElement)) {
            return;
        }

        setNavigationOpen(false);
        toggle.focus();
    });

    mobile.addEventListener("change", event => {
        setNavigationOpen(event.matches && navigation.contains(document.activeElement));

        if (!event.matches && document.activeElement === toggle) {
            navigation.querySelector("a:not(.header__nav-link--mobile)")?.focus();
        }
    });

    setNavigationOpen(mobile.matches && navigation.contains(document.activeElement));
    toggle.hidden = false;
})();
