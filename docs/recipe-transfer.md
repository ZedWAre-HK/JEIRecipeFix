# JEI Fabric Recipe Transfer

## Purpose

JEI can show server recipes after recipe synchronization, but its `+` button remains disabled unless the server advertises and handles JEI's recipe-transfer packets. Modded servers get this behavior from JEI's server-side mod. Paper-family servers cannot load that mod, so this plugin implements the narrow protocol surface required for normal crafting transfers.

## Current Verification

The implementation targets Fabric clients running JEI. It has been verified only on Purpur 1.21.11 with Fabric JEI 27.17.0.50. The original recipe-sync feature has its own broader compatibility range; this transfer bridge must be tested separately for every server core, Minecraft version, and JEI protocol version before being advertised for that combination.

If recipe transfer works on another combination, open a compatibility issue with the server core and build, Minecraft version, Fabric Loader version, JEI version, and the transfer actions tested. The maintainer can then verify the report and mark that combination as supported.

It advertises JEI's Fabric payload channels so JEI recognizes that a server supports recipe transfer, then handles these packets:

| Channel | Purpose |
| --- | --- |
| `jei:recipe_transfer` | Transfer one set of recipe ingredients. |
| `jei:recipe_transfer_counted` | Transfer ingredients with explicit per-slot counts. |

The remaining JEI channels are registered only to satisfy JEI's server capability detection. Their requests are intentionally ignored. The plugin never grants items, deletes items, or writes hotbar slots for JEI cheat-mode features.

## Server Behavior

For a valid packet, the bridge:

1. Checks every declared slot against the active `InventoryView`.
2. Ensures each operation stays within the packet's declared source and crafting-slot groups.
3. Takes a copy of every affected slot before changing inventory state.
4. Returns existing crafting-grid items to the player inventory.
5. Moves matching source stacks into the requested crafting slots.
6. Restores the snapshot if any validation or movement step fails.

Maximum transfer is deliberately conservative: it moves the largest number of complete recipe sets that fit in the declared source stacks and destination slots. Partial sets are not moved.

## Compatibility

The bridge is intended for normal Bukkit/Paper inventory layouts and has been exercised with the player 2x2 crafting grid and vanilla 3x3 crafting table. It does not claim compatibility with custom GUI menus, modded containers, or plugin inventories whose raw-slot layout differs from vanilla crafting.

Recipe synchronization remains separate from transfer handling. The existing `JEIRecipeFix` recipe-sync path provides vanilla, datapack, and Bukkit-registered recipes to compatible clients; transfer only moves items already present in the player's server inventory.

## Security Notes

Plugin messages are client-controlled input. The decoder therefore rejects malformed VarInts, trailing data, lists larger than 256 entries, non-positive or over-sized counts, and all out-of-range slots. Server-side inventory state is authoritative at every step.

## Verification

Run the automated checks:

```powershell
./gradlew test
```

Then verify with a Fabric JEI client on a Paper/Purpur server:

1. Open the player 2x2 crafting grid and a vanilla crafting table.
2. Use JEI's normal transfer and maximum-transfer actions.
3. Test missing ingredients, occupied crafting slots, full inventories, and invalid custom-container layouts.
