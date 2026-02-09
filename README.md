# Create: Backpackage

A Create addon mod that adds cardboard backpacks fitting the Create aesthetic. Because every Create pack uses Sophisticated Storage: Backpacks, why not integrate backpacks into Create's own cardboard and package system?

## Core Concept

Create-themed cardboard backpacks with modular upgrade system. Wearable (via Curios) and placeable as blocks, just like Sophisticated Backpacks.

**No durability/fuel mechanics** - those aren't fun. Once an upgrade is installed, it just works.

## Upgrade System

The backpack has upgrade slots that accept different contraption upgrades:

### 1. Magnet Upgrade ✅
- Pulls nearby dropped items into the backpack (8 block radius)
- Checks every 0.5 seconds for performance
- Respects vanilla pickup delay (won't grab items you just threw)
- Disabled while sneaking
- Recipe ideas: Electron Tube + Copper?

### 2. Void Upgrade
- Voids excess items
- **Recipe**: Brass Tunnel (consumes glue durability) + Lava Bucket (consumes lava, returns bucket)
- Very Create-authentic - teaches players Create mechanics through the recipe

### 3. Crafting Upgrade
- Full 3x3 crafting grid (like a crafting table)
- Auto-crafting and compression (2x2, nugget/ingot/block conversions)
- Recipe: Mechanical Press involved?

### 4. Linked Upgrade
- **This is the killer feature**
- Recipe: Packager + Frogport + Storage Link + Glue
- Taps into Create's logistics network
- Works with Create Storage (player's in-game name as delivery address)
- When in range of your chain transit network, the backpack's frogport retrieves packages sent via the network
- Essentially: remote base access while exploring

**Note**: Frogport is in base Create, not New Age (New Age is the electricity addon)

### 5. Auto-Feeder Upgrade
- Automatically eats food from the backpack when the player is hungry
- Configurable with whitelist/blacklist or Create-style filters
- Won't eat golden apples unless you want it to

### 6. Auto-Drinker Upgrade
- Automatically drinks potions from the backpack
- Configurable with whitelist/blacklist or Create-style filters
- Great for keeping fire resistance up while in the Nether

### 7. Slot Memory Upgrade
- Locks backpack slots to remember specific item types
- Only allows the memorized item in that slot (like a filter for each slot)
- Matches by item type, ignoring durability/NBT — so a half-broken pickaxe still goes in the pickaxe slot
- Useful for tool loadouts, sorted inventories, or keeping your backpack organized

### Upgrade Features (Planned)
- **Toggle system**: Enable/disable upgrades without removing them (e.g., sneak+keybind or GUI button)
- **Filter support**: Upgrades like feeder, drinker, and void will support Create-style filters or whitelist/blacklist configuration

## Storage Sizing

Base backpack needs at least the same amount of slots as a package

### Tier Ideas
- **Cardboard**:  1 upgrade slot
- **Reinforced Cardboard**: (maybe stack max increase), 2 upgrade slots
- **Industrial Cardboard**: (more stack max increase?) 3-4 upgrade slots

Progression through Create's material chain (basic cardboard → reinforced → brass-bound or something).

## Technical Notes

### Platform
- NeoForge for Minecraft 1.21.1
- Create as dependency
- Curios API for wearable slot

### Recipe Philosophy
- Use Create's existing items and mechanics
- Recipes should teach Create mechanics (like the void upgrade using brass tunnel + glue durability)
- Fit into Create's progression naturally

### Visual Design
- Cardboard aesthetic matching Create's existing cardboard armor
              (the packages are soooooooo cute)
- Worn, taped-together boxes on the player's back
- Distinct from Sophisticated Storage's clean leather look

## Development TODO

- [x] Set up basic backpack item
- [x] Implement inventory/container system
- [x] Item persistence (DataComponents.CONTAINER)
- [x] Make it wearable (Curios integration)
- [x] Keybind ("B") to open from Curios slot
- [x] Make it placeable as block
- [x] Implement upgrade slot system (1 slot, BackpackUpgradeItem interface)
- [x] Magnet Upgrade
- [ ] Upgrade toggle system
- [ ] Void Upgrade
- [ ] Crafting Upgrade
- [ ] Linked Upgrade
- [ ] Auto-Feeder Upgrade
- [ ] Auto-Drinker Upgrade
- [ ] Slot Memory Upgrade
- [ ] Filter/whitelist/blacklist system
- [ ] Design and implement recipes
- [x] Textures and models
- [ ] Test with Create's logistics network
- [x] Make a flake.nix for dev environment

## Why This Mod?

Every single Create pack, even small ones, includes Sophisticated Storage just for backpacks. There's clearly demand for portable storage in Create-heavy environments. A native Create solution that:
1. Fits the aesthetic
2. Integrates with Create's systems (especially the linked upgrade)
3. Uses Create's existing items/mechanics


## Future Expansion Ideas

- More upgrade types?
- Interaction with other Create systems (fluid storage?)
- Cosmetic upgrades (labels, paint, tape patterns)?
- Advanced logistics features building on the linked upgrade?

Keep it focused initially - get the core four upgrades working well before expanding.
