# Fabric 1.21.11 Carpet Mod

Two Fabric mods for Minecraft 1.21.11 that work together:

- **`server-mod`** - the actual gameplay/collision logic. Buildable Java mod, runs on
  the server (and the integrated server in singleplayer). Deliberately stays
  vanilla-registry-compatible (no new blocks/items) so unmodded clients can still
  connect - in the spirit of [Carpet](https://github.com/gnembon/fabric-carpet)'s
  "no client mod required" philosophy, even though this project doesn't depend on
  Carpet itself.
- **`client-visuals-mod`** - not really a "mod" in the logic sense. It's a thin,
  client-only Fabric mod whose only job is to bundle this project's collection of
  resource packs and auto-register them as built-in, toggleable packs, so players
  just install one jar instead of manually copying pack folders around.

Everything below was set up against the **actual, currently-live** FabricMC
`fabric-example-mod` template (branch `1.21.11`), fetched directly from GitHub
while scaffolding this repo - not from memory, since Fabric changed some
fundamentals for this version (see **Important toolchain changes** below).

## Project layout

```
.
├── server-mod/                  Java, environment: "*", server-side logic
│   └── src/main/java/com/example/collision/
│       ├── ExampleCollisionMod.java        ModInitializer entrypoint
│       └── mixin/ExampleServerLifecycleMixin.java   verified-working Mixin template
├── client-visuals-mod/          Java, environment: "client"
│   └── src/main/
│       ├── java/com/example/visuals/ExampleVisualsMod.java   registers bundled packs
│       └── resources/resourcepacks/example_retexture/         one pack per subfolder
└── gradle.properties            shared toolchain versions for both subprojects
```

Each subproject is a normal Fabric Loom Gradle module; the root `settings.gradle`
just includes both. Build everything with `./gradlew build`, or a single module
with `./gradlew :server-mod:build`.

## Requirements

- **JDK 21** (Minecraft 1.21.11 requires Java 21; confirmed present in this repo's
  build config via `sourceCompatibility`/`targetCompatibility`/`options.release`).
- **Gradle 9.5.1** via the included wrapper (`./gradlew` / `gradlew.bat`) - don't
  need Gradle installed separately.
- An IDE with Fabric support: **IntelliJ IDEA** (Loom integrates natively) or
  **VS Code** with the [Minecraft Development extension](https://marketplace.visualstudio.com/items?itemName=vscode-mc-dev.minecraft-development).
- Outbound internet access to `maven.fabricmc.net`, `libraries.minecraft.net`,
  and `piston-meta.mojang.com` the first time you build (Loom downloads and
  remaps the game jar and dependencies then caches them).

## Important toolchain changes for 1.21.11

Fetched straight from the live `FabricMC/fabric-example-mod` repo's `1.21.11`
branch while building this scaffold:

- **Mappings**: this is the *last* Minecraft version with Yarn/Intermediary at
  all - Fabric has stopped updating Yarn after 1.21.11 and moved to Mojang's own
  mappings. This project already uses `loom.officialMojangMappings()`, matching
  the official template, so classes are named the Mojang way (e.g.
  `net.minecraft.resources.Identifier`, `net.minecraft.server.MinecraftServer`)
  rather than Yarn's old package layout.
- **Loom plugin ID changed**: it's now
  `id 'net.fabricmc.fabric-loom-remap' version "1.17-SNAPSHOT"`, not the old
  `net.fabricmc.loom`. Both `build.gradle` files here use the new id.
- Versions pinned in root `gradle.properties` (from the live template, so they're
  known-good together): `loader_version=0.19.5`,
  `fabric_api_version=0.141.6+1.21.11`, `loom_version=1.17-SNAPSHOT`. Re-check
  current values at <https://fabricmc.net/develop> before shipping - Loom in
  particular is pinned to a `-SNAPSHOT`, which the official template also does,
  but you may want to lock to a dated release once one exists.

## How the two mods work together

`server-mod` doesn't register any new blocks/items - the "collision" behavior it
adds is meant to be built as Mixins into *existing* vanilla classes (see the
comments in `ExampleCollisionMod.java` and the verified Mixin pattern in
`ExampleServerLifecycleMixin.java`, which mirrors the official template's own
example mixin target). Because nothing new is registered, a player connecting
without any mods installed still works fine - they just won't see anything
different, since a resource pack can only reskin/re-sound/relabel things that
already exist, not add new registry entries.

`client-visuals-mod` supplies that reskin. Each folder under
`client-visuals-mod/src/main/resources/resourcepacks/` is a standalone, valid
Minecraft resource pack; `ExampleVisualsMod` registers each one via Fabric API's
`ResourceManagerHelper.registerBuiltinResourcePack(...)`
(`fabric-resource-loader-v0`) with `ResourcePackActivationType.DEFAULT_ENABLED`,
so it shows up already active in the resource pack menu but can still be toggled
off per-pack. `example_retexture` is a placeholder that just renames the Stick
item in `en_us.json` so you can confirm the pipeline is wired up in-game before
you add real textures/models/sounds.

To add another pack to the collection: drop a new folder with its own
`pack.mcmeta` + `assets/` under `resourcepacks/`, then add its folder name to
`BUNDLED_PACKS` in `ExampleVisualsMod.java`.

**Double-check `pack_format`** in each `pack.mcmeta` against
<https://minecraft.wiki/w/Pack_format> for your exact build - version numbering
changed recently (Fabric's own posts now reference things like "26.2") and this
scaffold couldn't reach the wiki to confirm the exact current value, so treat the
number that's there as a best-effort placeholder.

## Running in development

- `./gradlew :server-mod:runServer` - launches a dedicated test server with the
  mod loaded.
- `./gradlew :server-mod:runClient` / `./gradlew :client-visuals-mod:runClient` -
  launches a dev client with that module loaded, for singleplayer/LAN testing.
- Loom also generates a merged run configuration in IntelliJ if you import the
  project as a Gradle project.

## Publishing later

When you're ready to distribute:
- [Modrinth](https://modrinth.com) and [CurseForge](https://www.curseforge.com/minecraft)
  are the two mod hosts; both mods can go up as separate listings that link to
  each other, matching this repo's split.
- For the server, ship `server-mod`'s jar in the server's `mods/` folder as usual.
- For players, `client-visuals-mod`'s jar goes in `.minecraft/mods/` like any
  client mod - no manual resource pack copying needed, since it self-registers.
- If instead you want the server to *push* the resource pack automatically to
  vanilla clients (no client mod needed at all), host the zipped pack somewhere
  and set `resource-pack` / `resource-pack-sha1` (and `require-resource-pack` if
  you want to enforce it) in `server.properties` - a legitimate alternative to
  `client-visuals-mod` for a purely visual pack with no other client code.

## License

MIT, see `LICENSE`. The mod IDs (`examplecollision`, `examplevisuals`), package
names (`com.example.*`), and author fields are all placeholders - rename them
(find-and-replace `com.example`, `examplecollision`, `examplevisuals`) before
publishing.
