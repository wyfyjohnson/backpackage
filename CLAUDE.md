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

- **Items**: `ITEMS` DeferredRegister - contains `CARDBOARD_BACKPACK`, `MAGNET_UPGRADE`
- **Blocks**: `BLOCKS` DeferredRegister - contains `CARDBOARD_BACKPACK_BLOCK`
- **Block Entities**: `BLOCK_ENTITIES` DeferredRegister - contains `BACKPACK_BLOCK_ENTITY`
- **Menus**: `MENUS` DeferredRegister - contains `BACKPACK_MENU` (IMenuTypeExtension)
- **Creative Tabs**: `CREATIVE_MODE_TABS` DeferredRegister - single custom tab

### Backpack System Architecture

**Item Layer** (`CardboardBackpackItem.java`):
- Extends `BlockItem` (stack size 1) and implements `ICurioItem`, `Equipable`
- Right-click on block: places backpack. Right-click in air: opens GUI via `SimpleMenuProvider` (server-side only)
- Persistence via `DataComponents.CONTAINER` (`getInventoryFromStack` / `saveInventoryToStack`)
- `canEquipFromUse` returns false so right-click opens GUI instead of auto-equipping
- Equippable in vanilla chest slot via `Equipable` interface

**Block Layer** (`BackpackBlock.java`, `BackpackBlockEntity.java`):
- `BackpackBlock` extends `HorizontalDirectionalBlock`, implements `EntityBlock`
- Directional placement with custom VoxelShape hitboxes per facing
- Right-click opens the same `BackpackMenu` GUI via `BackpackLocation.InBlock`
- `setPlacedBy` loads inventory from the item stack into the block entity
- `playerWillDestroy` saves inventory back to the dropped item stack
- `BackpackBlockEntity` uses `ItemStackHandler` for inventory, persists via NBT

**Container Layer** (`BackpackMenu.java`):
- 24-slot storage (3 rows × 8 columns) + 1 upgrade slot (index 24)
- Recursion safety: prevents backpacks with contents from being placed inside backpacks (`mayPlace` override)
- Upgrade slot only accepts items implementing `BackpackUpgradeItem`
- Shift-click: upgrade items prioritize upgrade slot, regular items go to storage only
- Saving handled in `removed()`: block entities use `setChanged()`, item-based locations use `saveInventoryToStack`

**GUI Layer** (`BackpackScreen.java`):
- Client-side only, registered via `RegisterMenuScreensEvent` in `ClientModEvents`
- Texture path: `assets/create_backpackage/textures/gui/backpack.png`
- Custom image height (168px) to accommodate backpack storage layout

### Data Persistence
Inventory persistence is implemented using Minecraft 1.21's `DataComponents.CONTAINER`:
- `CardboardBackpackItem.getInventoryFromStack()` reads items from the component
- `CardboardBackpackItem.saveInventoryToStack()` writes items back
- `BackpackMenu.removed()` saves inventory to the backpack ItemStack when the menu closes

### Curios Integration & Networking
The backpack is wearable in the Curios "back" slot:
- **`BackpackLocation.java`**: Sealed interface with `InHand`, `InCurios`, `InBlock`, and `InChestSlot` variants, handles network serialization
- **`OpenBackpackFromCuriosPayload.java`**: Custom network payload for opening backpack from Curios slot
- **`CreateBackpackageClient.java`**: Registers "B" keybind, sends payload on keypress
- **`CreateBackpackage.handleOpenBackpackFromCurios()`**: Server-side handler that retrieves the backpack from Curios (with chest slot fallback) and opens the menu
- Curios slot defined in `data/curios/slots/back.json` and `data/curios/slots/body.json`, item tagged in `data/curios/tags/item/body.json`

### Upgrade System
The upgrade system uses a marker interface `BackpackUpgradeItem` and a single upgrade slot (index 24) in the backpack inventory. Upgrades are passive — behavior is driven by tick event handlers that check for installed upgrades.

**Implemented:**
- **`BackpackUpgradeItem`** (`BackpackUpgradeItem.java`): Marker interface for upgrade items
- **`MagnetUpgradeItem`** (`MagnetUpgradeItem.java`): Simple Item subclass implementing BackpackUpgradeItem
- **`MagnetUpgradeHandler`** (`MagnetUpgradeHandler.java`): Subscribes to `PlayerTickEvent.Post`, checks every 10 ticks for nearby item entities within 8 blocks, inserts into backpack storage slots (0-23), respects pickup delay, disabled while sneaking. Searches for backpack in: Curios back/body → chest slot → main hand → off hand.

**Planned Upgrades** (see README.md):
1. **Void Upgrade**: Void excess items (recipe: Brass Tunnel + Lava Bucket)
2. **Crafting Upgrade**: 3×3 crafting grid with auto-crafting
3. **Linked Upgrade**: Integrates with Create's logistics network via Frogport
4. **Auto-Feeder Upgrade**: Automatically eats food from backpack when hungry (with filter support)
5. **Auto-Drinker Upgrade**: Automatically drinks potions from backpack (with filter support)
6. **Slot Memory Upgrade**: Locks slots to specific item types (matches by item ID, ignoring durability/NBT for tools/armor)

**Planned Upgrade Features:**
- Toggle system: ability to enable/disable individual upgrades without removing them
- Filter support: some upgrades (feeder, drinker, void) will use Create-style filters or whitelist/blacklist configuration

## Dependencies

**Core Dependencies**:
- NeoForge 21.1.219 for Minecraft 1.21.1
- Create 6.0.9-215 (slim, non-transitive)
- Curios API 9.5.1+1.21.1 (for wearable "back" slot)
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

- ✅ Basic backpack item
- ✅ Inventory/container system (24 slots)
- ✅ Item persistence (DataComponents.CONTAINER)
- ✅ Curios integration (wearable in "back" slot)
- ✅ Keybind ("B") to open from Curios slot
- ✅ Basic textures/models
- ✅ Nix dev environment (flake.nix)
- ✅ Placeable as block (BackpackBlock + BackpackBlockEntity)
- ✅ Upgrade slot system (1 slot, accepts BackpackUpgradeItem)
- ✅ Magnet Upgrade (MagnetUpgradeItem + MagnetUpgradeHandler)
- ❌ Upgrade toggle system
- ❌ Void Upgrade
- ❌ Crafting Upgrade
- ❌ Linked Upgrade
- ❌ Auto-Feeder Upgrade
- ❌ Auto-Drinker Upgrade
- ❌ Slot Memory Upgrade
- ❌ Filter/whitelist/blacklist system for upgrades
- ❌ Recipes

## Key Technical Constraints

1. **Recursion Prevention**: Always prevent backpacks from being placed inside backpacks to avoid infinite nesting exploits
2. **Server-Side Menu Opening**: Menu opening logic must be server-side to prevent desync
3. **Client Event Registration**: Screen registration must use `@EventBusSubscriber` with `Dist.CLIENT` and `Bus.MOD`
4. **Java 21**: Project targets Java 21 (Mojang ships this with 1.21.1)
