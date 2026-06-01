package fr.horizonsmp.jeirecipefix.nms;

import org.bukkit.entity.Player;

public interface RecipeBridge {

    /** True when server internals were resolved and recipe sync can run. */
    boolean isAvailable();

    /** Number of recipes that will be sent (for status/logging). */
    int recipeCount();

    /** Encodes the full recipe set in the Fabric {@code fabric:recipe_sync} wire format. */
    byte[] buildFabricPayload();

    /** Encodes the full recipe set in the NeoForge {@code neoforge:recipe_content} wire format. */
    byte[] buildNeoForgePayload();

    /** Sends a pre-built Fabric payload to the player on the correct thread. */
    void sendFabric(Player player, byte[] payload);

    /** Sends a pre-built NeoForge payload (plus the tags packet) to the player on the correct thread. */
    void sendNeoForge(Player player, byte[] payload);
}
