# TierTag — Changelog

Every change shipped with each release. Read the newest section to decide whether to update the mod.

- Releases page: https://github.com/gqo6/TierTag/releases

## v1.2.1 — 2026-10-29

### Should I update?
Yes — if the End Game / Crystal icon renders as a plain rectangle or a white dot on your server.

### Fixed
- **End Game / Crystal icon redrawn as a proper item-style icon** (same visual language as the mace / diamond chestplate icons): an octahedron crystal with a thick dark outline, icy-cyan upper facets, ender-purple lower facets, white glint and a magenta core. Reads clearly at the real in-game glyph size (8px) — verified with an in-game screenshot.

## v1.2.0 — 2026-10-29

### Should I update?
Yes — if you play on servers where some player names render as colorful pixel glyphs inside the Tab list.

### Added
- **Plain readable names** (on by default): decorative Unicode names (`𝓕𝓻𝓮𝓪𝓷𝓴`-style) are rewritten to plain letters (`Frank`), private-use-area sticker characters and zero-width marks are stripped, and custom per-name fonts are cleared. Server rank colors are preserved.
- New command: `/tiertag plain` toggles it (also "Plain readable names" in Mod Menu).

## v1.1.0 — 2026-10-29

### Should I update?
Yes — especially if a stale jar (or a server resource pack) kept rendering the crystal as a white dot, or if you play on a Minecraft version other than 1.21.11.

### Added
- **Dedicated icon font** (`tiertag:tier_icons`): all glyphs live in the mod's own namespace and can no longer be shadowed by old jars, other icon mods, or server resource packs.
- **Wider version range**: works on Minecraft 1.21 through 26.3 (`"minecraft": ">=1.21"`).
- `/tiertag update` now always prints the exact version that is running — instant diagnostics.
- Update checker is crash-safe on 1.21.0/1.21.1 (the clickable link degrades to plain text instead of breaking).
- Mixins are soft (`defaultRequire: 0`) — a future refactor can cost one display surface instead of crashing the game.

### Fixed
- The End Game/Crystal white-dot root cause: glyph-provider conflicts inside the shared `default` font; icons are now isolated behind the dedicated font.

## v1.0.1 — 2026-10-29

### Should I update?
Yes if you run v1.0.0 and the crystal renders as a solid block.

### Added
- Automated GitHub Actions release pipeline: any `v*` tag builds the jar and attaches it to the GitHub release.
- Deterministic in-game visual harness (6 kit screenshots) proving icon rendering before each release.

### Fixed
- End Game/Crystal texture: logical 8×8 grid upscaled to solid 4×4 texel blocks at 32×32 — survives any texture filtering.

## v1.0.0 — 2026-10-25

### Initial release
- Verified MCPVP tiers (HT/MT/LT) above heads, in the Tab list and in chat.
- Gamemode icons: Mace, Sword, Crystal, Pot, Spear, Shield, Bow, Golden Apple, Creeper, Kit Cart and more.
- GitHub update checker with a clickable download link in chat.
- Full command set: `/tiertag toggle | tab | icons | reload | update | clearcache | lookup | search`.
- Full Mod Menu settings (tier colors, render distance, cache duration...).
- Live MCPVP HTML parsing — never invents unverified tiers.
