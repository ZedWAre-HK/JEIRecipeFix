# JEIRecipeFix

**Makes recipe-viewer mods work again on Paper, Purpur and Folia servers.**

Since Minecraft **1.21.2**, servers no longer send recipe data to clients. On a plugin-based server (Paper / Purpur / Folia) this means **JEI**, **REI** and **EMI** show no recipes at all — the mods simply can't see what the server can craft. JEI's own recommendation is to install JEI on the server, but JEI is a mod and cannot run on a plugin server.

**JEIRecipeFix fixes this from the server side.** It sends your server's recipes to the recipe-viewer mods so recipe lookups work again — with **no client mod to install** and **no mod loader on the server**. Players just connect with JEI (or REI / EMI) and it works.

## Features

- Restores recipe lookups in **JEI, REI and EMI**.
- **Zero setup** — install the plugin and players just connect; recipes are sent automatically on join.
- Works with **Fabric** and **NeoForge** clients.
- Covers **vanilla, datapack and other plugins'** recipes, and refreshes automatically after a datapack reload.
- A **single jar** for the whole **1.21.2 → 26.1.2** range.
- Lightweight and dependency-free; stays silent for vanilla clients.

## Experimental JEI Recipe Transfer

This branch also implements the server side of JEI's Fabric recipe-transfer protocol on Paper, Purpur, and Folia. It lets a Fabric client with JEI move matching ingredients from its inventory into the player crafting grid or a vanilla crafting table. Recipe transfer is currently verified only on Purpur 1.21.11 with Fabric JEI 27.17.0.50. Other server-core, Minecraft, and JEI combinations require compatibility testing before they are claimed as supported. If a combination works, please open a compatibility issue so the maintainer can verify and mark it as supported.

The bridge advertises JEI's Fabric channels and accepts `jei:recipe_transfer` and `jei:recipe_transfer_counted`. Every transfer is validated against the currently open Bukkit inventory before any item is moved. Invalid packets, invalid slot references, over-sized lists, invalid counts, and incompatible destination stacks are rejected. If a transfer fails after the crafting grid has changed, the affected slots are restored from a snapshot.

This is intentionally scoped to recipe transfer only. It does not enable JEI cheat actions, item deletion, hotbar writes, or item giving on a plugin server. See [Recipe Transfer](docs/recipe-transfer.md) for protocol notes, compatibility, and limitations.

## How it works

The plugin reads the recipes your server already knows and delivers them to the client the same way a mod loader normally would. It does **not** change crafting, add content, or affect gameplay — it only restores the recipe information that recipe-viewer mods need in order to display it.

## Requirements

- A **Paper**, **Purpur** or **Folia** server on Minecraft **1.21.2–26.1.2**.
- Each player uses a **Fabric** or **NeoForge** client with **JEI**, **REI** or **EMI** installed (as they already would).

## Commands

| Command | Description |
| --- | --- |
| `/jrf info` | Show status: recipe count, online players, sync state. |
| `/jrf resync [player\|all]` | Re-send recipes (useful after datapack changes). |
| `/jrf reload` | Reload the configuration. |

`/jeirecipefix` is the full command; `/jrf` is the alias. All commands require the `jeirecipefix.admin` permission (operators by default).

## Configuration

`config.yml` offers a few simple toggles:

```yaml
enabled: true
sync-on-join: true
sync-on-datapack-reload: true
debug: false
```

## Note

This is an unofficial, third-party plugin. It is **not** affiliated with, or made by, the authors of JEI, REI or EMI.
