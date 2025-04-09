function format_date(date, locale = "nb-no") {
    return `${date.toLocaleDateString(locale)} ${date.toLocaleTimeString(locale)}`;
}

function set_detailed_view(key) {
    if (typeof search_results == "undefined")
        return;
    const rp = search_results[key];
    const detailed_view_container =
        document.getElementById("detailed_view");
    if (detailed_view_container == null || rp == null)
        return;

    const public_sector_str = rp["public_sector"] ? "offentlig" : "privat";
    const active_str = rp["active"] ? "ja" : "nej";

    const t_created      = format_date(new Date(parseInt(rp["created_ms"])));
    const t_last_updated = format_date(new Date(parseInt(rp["last_updated_ms"])));

    const basic_info_table = `
        <table>
            <tbody>
                <tr>
                    <th>UUID</th>
                    <td style="font-family: monospace;"> ${rp["id"]} </td>
                </tr>
                <tr>
                    <th>Org.nr.</th>
                    <td>${rp["org_nr"]}</td>
                </tr>
                <tr>
                    <th>Navn</th>
                    <td>${rp["name"]}</td>
                </tr>
                <tr>
                    <th>Sektor</th>
                    <td>${public_sector_str}</td>
                </tr>
                <tr>
                    <th>Opprettet</th>
                    <td>${t_created}</td>
                </tr>
                <tr>
                    <th>Sidst oppdatert</th>
                    <td>${t_last_updated}</td>
                </tr>
                <tr>
                    <th>Aktiv</th>
                    <td>${active_str}</td>
                </tr>
            </tbody>
        </table>`;

    const entitlement_rows =
        rp["relying_party_entitlements"]
            .map(entitlement => `<tr><td>${entitlement["entitlement"]}</td></tr>`)
            .join("");
    const eaa_rows =
        rp["relying_party_eaas"]
            .map(eaa => `<tr><td>${eaa["namespace"]}</td>
                             <td>${eaa["intent"]}</td></tr>`)
            .join("");

    let entitlements_table =
        entitlement_rows.length <= 0
            ? `Ingen registrerte entitlements for ${rp["name"]}`
            : `<table>
                   <thead>
                       <tr><th>Entitlement</th></tr>
                   </thead>
                   <tbody>
                       ${entitlement_rows}
                   </tbody>
               </table>`;

    let eaa_table =
        eaa_rows.length <= 0
            ? `Ingen registrerte EAA'ere for ${rp["name"]}`
            : `<table>
                   <thead>
                       <tr><th>Namespace</th><th>Intent</th></tr>
                   </thead>
                   <tbody>
                       ${eaa_rows} 
                   </tbody>
               </table>`;

    detailed_view_container.innerHTML = `
        <h2>Detaljer for ${rp["name"]}</h2>
        <div class="vertical-headers-table">
            ${basic_info_table}
        </div>

        <h3>Rettigheter</h3>
        <div class="fixed-headers-table" style="height: 20vh;">
            ${entitlements_table}
        </div>

        <h3>EAA'ere</h3>
        <div class="fixed-headers-table" style="height: 20vh;">
            ${eaa_table}
        </div>
    `;
}
