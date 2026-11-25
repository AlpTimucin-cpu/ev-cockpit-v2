# Mola host tab integration — authorized scope (2026-11-25)

User authorizes changing Mola only to add a dedicated EV Cockpit menu tab. Preserve all existing tabs, visual design, media behavior, and original APK backup. The EV Cockpit side must not add a reverse Mola tab. No Android Auto or driving safety restrictions are changed.

## Verified binary evidence
Original Mola 4.8.3 APK SHA-256:
124e9121c41a624893b0f0794854f3d5ba4fc8a29a15b051c3f4bc376d8fd149

DEX class definitions (not just string references):
- classes.dex: me.aap.fermata.addon.FermataActivityAddon: getAddonId, getInfo, handleIntent, install, start, stop, uninstall.
- classes3.dex: me.aap.fermata.addon.FermataFragmentAddon: createFragment, getAddonId, getFragmentId, getInfo, handleIntent, preload, install, start, stop, uninstall.
- classes3.dex: me.aap.fermata.ui.view.FermataNavBarView.
- classes3.dex: me.aap.fermata.ui.activity.MainActivityDelegate: createFragment, getNavBar, showFragment.
- classes3.dex: me.aap.utils.ui.view.CustomizableNavBarMediator: addItem, getItems, createOverlayMenu, fragmentChanged.
- classes3.dex: me.aap.utils.ui.view.PrefNavBarMediator: getPref, reload.
- classes5.dex: me.aap.fermata.addon.radio.RadioAddon: createFragment, getAddonId, getInfo, install.
- classes6.dex: me.aap.fermata.addon.web.WebBrowserAddon: createFragment, getAddonId, getInfo, install.

These symbols identify candidate extension points, not an externally supported SDK or proof that a new plugin can be installed. Actual call graph, registration, resource IDs, layout and lifecycle must be inspected before any patch.

## Integration contract
EV Cockpit package: app.evcockpit. Existing explicit user navigation link: evcockpit://open/dashboard (route, plan, obd, settings also supported).
The receiving activity validates whitelisted URI, does not auto-start OBD, transfer location or invoke media.
A host tab that only invokes that link launches a separate Activity; it is not an embedded native fragment. To satisfy the requested embedded tab, a supported Mola fragment/module implementation or a verified in-process client integration is required. Do not label a shortcut as embedding.
Do not expose driving-restricted media, bypass projection restrictions, or change the Mola signing/trust behavior.

## Build blocker
The release repository moris545454/mola-apk README describes APK download; no build.gradle, settings.gradle(.kts), app/build.gradle or LICENSE was found at those paths. The original APK was available for static DEX inspection, but local apktool, jadx, smali and Android SDK signing/build tools were not available, and outbound github.com DNS failed. Therefore no modified Mola APK was produced. Do not publish the original or modified third-party binary to a public repository without appropriate authorization.

## Acceptance gates
1. Obtain actual compatible source/build inputs or a verified reproducible APK modification toolchain. Keep original unchanged and record source hash.
2. Inspect addon registration/call graph and choose a unique resource identifier; add only one EV Cockpit tab. Use existing nav styling.
3. Check source/DEX/resource/manifest diff; reject unrelated modifications. Test original tabs and existing controls. Preserve existing safety policies.
4. Build, sign with an authorized test key, verify signature and alignment. A differently signed APK cannot update the vendor-signed installation without uninstalling it; data loss must be considered.
5. Test parked on POCO F5: new tab, map, touch, back, orientation, real route lookup, original Mola pages, process restoration. Device evidence required before claiming integration.
