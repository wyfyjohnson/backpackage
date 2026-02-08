# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Create: Backpackage is a NeoForge mod for Minecraft 1.21.1 that adds Create-themed cardboard backpacks. The mod integrates portable storage into Create's existing cardboard/package system with a modular upgrade system. Backpacks are wearable (via Curios API) and placeable as blocks.

**Key Design Philosophy**: No durability/fuel mechanics. Once an upgrade is installed, it just works. Recipes teach Create mechanics (e.g., void upgrade uses Brass Tunnel consuming glue durability + Lava Bucket).

## Build & Development Commands

### Basic Commands
```bash
./gradlew build          # Build the mod JAR
./gradlew runClient      # Launch Minecraft client with mod loaded
./gradlew runServer      # Launch Minecraft server with mod loaded
./gradlew runData        # Generate data (recipes, tags, models, etc.)
```

### Development with Nix
This project includes a `flake.nix` for reproducible dev environment:
```bash
nix develop             # Enter dev shell with JDK 21, Gradle, graphics libs
```

### Project Structure
- Java source: `src/main/java/dev/wyfy/createbackpackage/`
- Resources: `src/main/resources/assets/create_backpackage/`
- Generated resources: `src/generated/resources/`
- Mod configuration: `gradle.properties`

## Architecture

### Registration System
The mod uses NeoForge's `DeferredRegister` pattern for all registrations (items, menus, creative tabs). All registration happens in `CreateBackpackage.java`:

- **Items**: `ITEMS` DeferredRegister - currently contains `CARDBOARD_BACKPACK`
- **Menus**: `MENUS` DeferredRegister - contains `BACKPACK_MENU` (IMenuTypeExtension)
- **Creative Tabs**: `CREATIVE_MODE_TABS` DeferredRegister - single custom tab

### Backpack System Architecture

**Item Layer** (`CardboardBackpackItem.java`):
- Extends `Item` with stack size of 1
- Right-click opens GUI via `SimpleMenuProvider`
- Currently opens menu on server-side only

**Container Layer** (`BackpackMenu.java`):
- 24-slot storage (3 rows × 8 columns)
- Recursion safety: prevents backpacks from being placed inside backpacks (see `mayPlace` override in slot)
- Upgrade slots planned but not yet implemented (see TODO at line 38-39)
- Shift-click (quick move) support for transferring items between backpack and player inventory

**GUI Layer** (`BackpackScreen.java`):
- Client-side only, registered via `RegisterMenuScreensEvent` in `ClientModEvents`
- Texture path: `assets/create_backpackage/textures/gui/backpack.png`
- Custom image height (168px) to accommodate backpack storage layout

### Data Persistence
**IMPORTANT**: The current implementation does NOT persist backpack inventory data. The `ItemStackHandler` in `BackpackMenu` is created fresh each time the GUI opens. To implement persistence, you must:
1. Add NBT serialization to `CardboardBackpackItem` to store the `ItemStackHandler`
2. Pass the saved inventory data when creating `BackpackMenu`
3. Serialize back to NBT when the menu closes

### Planned Upgrade System
Four planned upgrades (see README.md lines 11-36):
1. **Magnet Upgrade**: Item pickup/collection
2. **Void Upgrade**: Void excess items (recipe: Brass Tunnel + Lava Bucket)
3. **Crafting Upgrade**: 3×3 crafting grid with auto-crafting
4. **Linked Upgrade**: The killer feature - integrates with Create's logistics network via Frogport, allowing remote base access while exploring

Upgrade slots are defined (`UPGRADE_SLOTS = 1`) but not yet implemented in the GUI.

## Dependencies

**Core Dependencies**:
- NeoForge 21.1.219 for Minecraft 1.21.1
- Create 6.0.9-215 (slim, non-transitive)
- Curios API 9.5.1+1.21.1 (for wearable slot - not yet integrated)
- Registrate MC1.21-1.3.0+67 (Create's registration library)

**Build-time Dependencies**:
- Parchment 2024.11.17 mappings for better parameter names
- Flywheel (Create's rendering engine) - compileOnly API, runtimeOnly impl

All custom Maven repositories are configured in `build.gradle` (Create Maven, Curios, Registrate, CurseMaven).

## Mod ID & Package Structure

- **Mod ID**: `create_backpackage` (must match everywhere)
- **Base Package**: `dev.wyfy.createbackpackage`
- **Main Mod Class**: `CreateBackpackage` (annotated with `@Mod`)

## Current Implementation Status

Based on README.md TODO (lines 68-79):
- ✅ Basic backpack item
- ✅ Inventory/container system (24 slots)
- ❌ Curios integration (wearable) - dependency present but not used
- ❌ Placeable as block
- ❌ Upgrade slot system (structure exists, not functional)
- ❌ Individual upgrades
- ❌ Recipes
- ✅ Basic textures/models

## Key Technical Constraints

1. **Recursion Prevention**: Always prevent backpacks from being placed inside backpacks to avoid infinite nesting exploits
2. **Server-Side Menu Opening**: Menu opening logic must be server-side to prevent desync
3. **Client Event Registration**: Screen registration must use `@EventBusSubscriber` with `Dist.CLIENT` and `Bus.MOD`
4. **Java 21**: Project targets Java 21 (Mojang ships this with 1.21.1)
