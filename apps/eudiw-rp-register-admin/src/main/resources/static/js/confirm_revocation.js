document.querySelectorAll(".confirm-revocation").forEach(form => {
    form.addEventListener("submit", event => {
        if (!window.confirm("Er du sikker på at du vil revokere dette sertifikatet?")) {
            event.preventDefault();
        }
    });
});
