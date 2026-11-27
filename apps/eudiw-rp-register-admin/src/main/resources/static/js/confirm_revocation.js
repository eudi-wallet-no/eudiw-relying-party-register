document.querySelectorAll(".confirm-revocation").forEach(form => {
    form.addEventListener("submit", event => {
        if (!window.confirm("Er du sikker på at du vil revokere dette sertifikatet?")) {
            event.preventDefault();
        }
    });
});

document.querySelectorAll(".confirm-party-revocation").forEach(form => {
    form.addEventListener("submit", event => {
        if (!window.confirm("Er du sikker på at du vil revokere brukarstaden? Dette kan ikkje angrast.")) {
            event.preventDefault();
        }
    });
});
