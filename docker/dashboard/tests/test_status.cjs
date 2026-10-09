"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

class Element {
    constructor() {
        this.children = [];
        this.textContent = "";
        this.className = "";
        this.hidden = false;
    }
    append(...elements) {
        for (const element of elements) {
            element.parent = this;
            this.children.push(element);
        }
    }
    remove() {
        this.parent.children = this.parent.children.filter((element) => element !== this);
    }
}

async function loadStatus() {
    const grid = new Element();
    const summary = new Element();
    let payload = { updatedAt: Math.floor(Date.now() / 1000), services: [
        { id: "example", title: "Example", description: "Description", link: "http://localhost:1234/", ready: true },
    ] };
    let fail = false;
    let interval;
    const context = {
        document: { getElementById: (id) => id === "service-grid" ? grid : summary,
                    createElement: () => new Element() },
        Date, Map, Number, AbortSignal,
        fetch: async () => {
            if (fail) throw new Error("network error");
            return { ok: true, json: async () => payload };
        },
        window: { setInterval: (fn, milliseconds) => {
            assert.equal(milliseconds, 5000);
            interval = fn;
        } },
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, "../static/status.js"), "utf8"), context);
    await new Promise(setImmediate);
    return { grid, summary, update: () => interval(),
             payload: () => payload, setPayload: (value) => { payload = value; },
             setFailure: (value) => { fail = value; } };
}

test("fresh readiness updates preserve cards and links", async () => {
    const app = await loadStatus();
    const card = app.grid.children[0];
    assert.equal(card.children[1].textContent, "Klar");
    assert.equal(card.children[3].href, "http://localhost:1234/");

    app.payload().services[0].ready = false;
    await app.update();
    assert.equal(app.grid.children[0], card);
    assert.equal(card.children[1].textContent, "Ikke klar");
    assert.equal(card.children[3].hidden, false);
});

test("stale status and fetch failure show unknown, then recover", async () => {
    const app = await loadStatus();
    const status = app.grid.children[0].children[1];
    app.payload().updatedAt -= 30;
    await app.update();
    assert.equal(status.textContent, "Ukjent");

    app.payload().updatedAt = Math.floor(Date.now() / 1000);
    await app.update();
    assert.equal(status.textContent, "Klar");
    app.setFailure(true);
    await app.update();
    assert.equal(status.textContent, "Ukjent");
    app.setFailure(false);
    await app.update();
    assert.equal(status.textContent, "Klar");
});

test("unmeasured and malformed status cannot appear ready", async () => {
    const app = await loadStatus();
    const status = app.grid.children[0].children[1];
    app.payload().services[0].ready = null;
    await app.update();
    assert.equal(status.textContent, "Ukjent");
    app.setPayload({ services: [] });
    await app.update();
    assert.equal(status.textContent, "Ukjent");
    assert.match(app.summary.textContent, /Kunne ikke hente/);
});
