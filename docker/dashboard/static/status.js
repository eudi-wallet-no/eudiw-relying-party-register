"use strict";

const grid = document.getElementById("service-grid");
const summary = document.getElementById("status-summary");
const cards = new Map();
let inFlight = false;

function renderServices(services, fresh) {
    for (const service of services) {
        if (!cards.has(service.id)) {
            const card = document.createElement("li");
            card.className = "service-card";
            const title = document.createElement("h2");
            title.className = "service-card__name";
            const status = document.createElement("p");
            const description = document.createElement("p");
            description.className = "service-card__description";
            const link = document.createElement("a");
            link.className = "service-card__link";
            card.append(title, status, description, link);
            grid.append(card);
            cards.set(service.id, { card, title, status, description, link });
        }

        const elements = cards.get(service.id);
        elements.title.textContent = service.title;
        elements.description.textContent = service.description;
        elements.link.href = service.link;
        elements.link.textContent = "Åpne tjenesten";
        elements.link.hidden = !service.link;
        const known = fresh && typeof service.ready === "boolean";
        elements.status.textContent = "Ukjent";
        elements.status.className = "service-status";
        if (known) {
            elements.status.textContent = service.ready ? "Klar" : "Ikke klar";
            elements.status.className += service.ready ? " service-status--up" : " service-status--down";
        }
    }

    for (const [id, elements] of cards) {
        if (!services.some((service) => service.id === id)) {
            elements.card.remove();
            cards.delete(id);
        }
    }
}

async function updateStatus() {
    if (inFlight) {
        return;
    }
    inFlight = true;
    try {
        const response = await fetch("/status.json", { cache: "no-store", signal: AbortSignal.timeout(3000) });
        if (!response.ok) {
            throw new Error("Statusfilen er utilgjengelig");
        }
        const data = await response.json();
        if (!Array.isArray(data.services) || !Number.isFinite(data.updatedAt)) {
            throw new Error("Ugyldig statusfil");
        }
        const age = Date.now() / 1000 - data.updatedAt;
        const fresh = data.updatedAt > 0 && age >= -5 && age < 15;
        renderServices(data.services, fresh);
        if (fresh) {
            summary.textContent = "Sist sjekket " + new Date(data.updatedAt * 1000).toLocaleTimeString("nb-NO");
        } else {
            summary.textContent = "Status er ukjent – venter på oppdaterte sjekker.";
        }
    } catch (error) {
        for (const { status } of cards.values()) {
            status.textContent = "Ukjent";
            status.className = "service-status";
        }
        summary.textContent = "Kunne ikke hente status. Prøver igjen automatisk.";
    } finally {
        inFlight = false;
    }
}

updateStatus();
window.setInterval(updateStatus, 5000);
