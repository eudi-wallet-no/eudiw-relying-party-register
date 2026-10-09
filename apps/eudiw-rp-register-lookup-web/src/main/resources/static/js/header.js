(() => {
    // Header-meny toggle
    const toggle = document.getElementById("header-nav-toggle");
    const toggleLabel = document.getElementById("header-nav-toggle-label");
    const navigation = document.getElementById("header-navigation");
    if (toggle && navigation) {
        function setNavigationOpen(open) {
            toggle.setAttribute("aria-expanded", String(open));
            const label = open ? "Lukk" : "Meny";
            toggle.setAttribute("aria-label", label);
            if (toggleLabel) toggleLabel.textContent = label;
            navigation.classList.toggle("header__nav--open", open);
        }
        toggle.addEventListener("click", () => {
            const expanded = toggle.getAttribute("aria-expanded") === "true";
            setNavigationOpen(!expanded);
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && toggle.getAttribute("aria-expanded") === "true") {
                setNavigationOpen(false);
                toggle.focus();
            }
        });
    }
    // Skip-link: flytt fokus til main uten å kreve designsystemet-web
    const skipLink = document.querySelector('a.ds-skip-link, a.ds-skiplink');
    const main = document.getElementById('hovudinnhald');
    if (skipLink && main) {
        skipLink.addEventListener('click', (e) => {
            e.preventDefault();
            main.setAttribute('tabindex', '-1');
            main.focus({ preventScroll: true });
            const headerOffset = 84;
            const top = main.getBoundingClientRect().top + window.scrollY - headerOffset;
            window.scrollTo({ top, behavior: 'instant' in window ? 'instant' : 'auto' });
            history.pushState(null, '', '#hovudinnhald');
        });
    }
    // Merk aktiv menypunkt med border-bottom – matcher referansen (svarte lenker, enkel hover)
    const navLinks = document.querySelectorAll('#header-navigation .header__nav-link');
    if (navLinks.length) {
        const path = window.location.pathname;
        navLinks.forEach((a) => {
            const href = a.getAttribute('href');
            if (!href) return;
            let isActive = false;
            if (href === '/') {
                isActive = path === '/';
            } else if (href === '/relying-parties') {
                isActive = path === '/relying-parties' || path.startsWith('/details');
            } else if (href === '/credentials-catalogue') {
                isActive = path.startsWith('/credentials-catalogue') || path.startsWith('/credential');
            } else {
                isActive = path === href || path.startsWith(href + '/');
            }
            if (isActive) a.setAttribute('aria-current', 'page');
            else a.removeAttribute('aria-current');
        });
    }
})();