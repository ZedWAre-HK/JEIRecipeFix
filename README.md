# JEIRecipeFix

Makes recipe-viewer mods work again on Paper, Purpur and Folia servers.

Since Minecraft 1.21.2, servers no longer send recipes to clients, so **JEI**, **REI** and **EMI** show nothing on a plugin-based server. JEIRecipeFix sends the server's recipes to those mods so recipe lookups work again — with **no client mod** and **no server mod loader**.

## Features

- Restores recipe lookups in JEI, REI and EMI.
- Works automatically: install the plugin, players just connect.
- Supports Fabric and NeoForge clients.
- Covers vanilla, datapack and other plugins' recipes; updates after a datapack reload.

## Supported versions

Minecraft 1.21.2 → 26.1.2, on Paper / Purpur / Folia.

## Commands

- `/jrf resync [player|all]` — re-send recipes.
- `/jrf reload` — reload the configuration.
- `/jrf info` — show status.

## Notes

This is an unofficial, third-party plugin. It is not made by the authors of JEI, REI or EMI.
