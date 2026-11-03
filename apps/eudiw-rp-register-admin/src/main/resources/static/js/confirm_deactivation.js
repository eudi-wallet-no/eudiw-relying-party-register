document.querySelectorAll(".confirm-deactivation").forEach(form => {
    form.addEventListener("submit", event => {
        if (!window.confirm("Er du sikker på at du vil deaktivere tenesta og instansane?")) {
            event.preventDefault();
        }
    });
});
