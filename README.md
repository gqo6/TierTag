# TierTag

A client-side Fabric mod for **Minecraft Java 1.21.11** that shows each player's verified
**MCPVP.com** PvP tier — with the gamemode's real item icon — merged directly into the
player's name everywhere it appears: above their head, in the Tab list, and in chat.


<img width="680" height="57" alt="image" src="https://github.com/user-attachments/assets/00707afb-7941-4a7b-b8dc-1c02c0d61eb2" />



The rendering approach (font-glyph icons embedded in the display name) is inspired by
uku3lig's open-source [TierTagger](https://github.com/uku3lig/TierTagger) — many thanks
for the clean design.

## How it works

**Icons as font glyphs.** The mod injects bitmap font providers into the vanilla
`default` font (private-use code points `\uEB01`–`\uEB0F` — a range that does not
collide with other popular tier mods, so both can be installed together; the
End Game/Crystal glyph lives at `\uEB0F`). Each glyph texture is 32×32 (resolved
to the text line height), generated from official renders with an alpha-aware
resampler; the End Game/Crystal icon is a hand-composed end crystal
(glass cube + magenta core + fire + bedrock pedestal). Because the icon is a text
glyph, it renders inline with the name — same line, same background, same billboard,
everywhere.

**Tag as part of the name.** A mixin on `PlayerEntity.getDisplayName` prepends
`[icon]TIER | ` to the name (gray separator, tier colored by the official MCPVP
palette). The Tab list gets the same treatment via `PlayerListHud.getPlayerName`.
No custom render math, no positioning bugs, no overlap: the tag is literally part
of the name text.

**Icons per gamemode** (sourced from MCPVP's own official kit icons at
`mcpvp.com/rankings/vanilla-hq/*.png`, alpha-aware downscaled to clean 8×8
pixel-art glyphs; UHC uses the vanilla golden apple): End Game/Crystal → End
Crystal, Mace → Mace, Sword → Diamond Sword, Pot → Splash Potion, Netherite Pot →
Netherite Sword, SMP/Diamond SMP → Diamond Chestplate, Late Game → Netherite
Chestplate, UHC → Golden Apple, Spear → Spear (MCPVP icon), Shield → Shield,
Bow → Bow, Early Game → Early (MCPVP icon), Creeper → Creeper, Cart → Cart.

## Data (no invented tiers)

MCPVP.com has no JSON API (Laravel Livewire site); the real tier data is
server-rendered into each player's public profile page
(`https://www.mcpvp.com/@<username>`), which this mod fetches asynchronously and
parses:

- per-kit tiers from the profile "Rankings" grid (`data-kit="mace"` +
  `data-tier-code="LT6"`),
- the overall tier from the `overall-tier` span,
- the official tier palette (`HT1=#FFDA55` … `ST6=#383838`) as default colors.

Tier grammar: `HT1–HT6, MT1–MT6, LT1–LT6, ST6` (lower = better). Kits whose current
tier is `???` are treated as *no verified tier* — nothing is invented. Nonexistent
players (HTTP 302) are negative-cached.

**Highest Tier mode** shows the player's *best verified per-kit tier* (e.g. a
player whose kits include HT3 displays HT3 with that kit's icon — even if MCPVP's
overall-points tier is lower). Any specific gamemode can be selected instead.

**Performance:** async HTTP off the render thread, in-flight dedupe, TTL cache
(10 min default), negative cache, exponential backoff, configurable min request
interval (500 ms default). Lookups only happen for players you actually see.

## Commands

- `/tiertag toggle` — enable/disable tags
- `/tiertag icons` — toggle icons
- `/tiertag tab` — toggle Tab list tiers
- `/tiertag lookup <player>` — print a player's MCPVP tiers in chat
- `/tiertag reload` — reload config
- `/tiertag clearcache` — drop all cached tiers
- `/tiertag update` — check GitHub for a new release now

## Update checker (GitHub)

On every launch the mod queries the GitHub Releases API for
`updateCheckRepo` (default `gqo6/TierTag`). If a newer release exists, you get a
chat message with the latest version and a clickable **[Download]** link to the
release page when you join a world. `/tiertag update` re-checks on demand.

Publish a new release on GitHub like this:

1. Push the source to `github.com/gqo6/TierTag`.
2. Build the jar (`gradlew build`) and create a GitHub Release with a semver tag
   such as `v1.1.0` — attach the jar from `build/libs/`.
3. Everyone running the mod gets the chat notification on next launch.

Note: Minecraft cannot hot-swap loaded mods, so the notification links to the
release page rather than replacing the jar in-place.

## Configuration

Mod Menu → TierTag, or `config/tiertag.json`:

| Key | Default | Meaning |
|---|---|---|
| `enabled` / `showIcons` / `showAboveHead` / `showInTabList` | `true` | toggles |
| `displayMode` | `HIGHEST` | `HIGHEST` or any kit slug (`mace`, `sword`, …) |
| `tagSeparator` | `\|` | separator between tier and name |
| `maxRenderDistance` | `64` | blocks; lookups beyond this are skipped |
| `cacheDurationSeconds` / `negativeCacheSeconds` / `requestIntervalMs` | `600 / 120 / 500` | network behavior |
| `tierColors` | MCPVP palette | per-tier `#RRGGBB` |
| `checkUpdates` / `updateCheckRepo` | `true` / `gqo6/TierTag` | GitHub release check |

## Building

JDK 21 toolchain; Fabric Loom 1.19.x needs a Java 25 runtime for the Gradle daemon
(see `org.gradle.java.home` in `gradle.properties`).

```
gradlew build
```

The jar lands in `build/libs/tiertag-1.0.0.jar`. A visual smoke test is
included: `gradlew runClientGameTest` boots the client, opens a world, holds Tab and
captures a screenshot verifying the glyph + tier render.

## Verification status

- MCPVP HTML schema + tier grammar live-verified (fixtures in `src/test/resources`,
  `gradlew test` green).
- Font glyphs verified visually in-game (client game test screenshot shows
  `[mace icon] HT1 | Dev` in the Tab list).
- Nameplate rendering uses the exact same `getDisplayName` text — verified against
  the decompiled 1.21.11 `EntityRenderer`/`PlayerEntityRenderer` sources.
- Live multiplayer behavior (real lookups against MCPVP while playing) needs a real
  account — use `/tiertag lookup <name>` to confirm lookups work on your machine.

## Privacy / rate limiting

Plain HTTPS GETs to public MCPVP profile pages with a custom User-Agent, only for
players within `maxRenderDistance`. Keep `requestIntervalMs` at or above its default.

## License

MIT
