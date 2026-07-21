package fr.horizonsmp.jeirecipefix.nms;

import org.bukkit.entity.Player;

public interface RecipeBridge {
    boolean isAvailable();
    int recipeCount();
    byte[] buildFabricPayload();
    byte[] buildNeoForgePayload();
    void sendFabric(Player player, byte[] payload);
    void sendNeoForge(Player player, byte[] payload);
    default Object createFabricPacket(byte[] payload) {
        throw new UnsupportedOperationException("Fabric configuration sync is unavailable");
    }

    default void advertiseFabricRecipeSync(Object configurationConnection) {
        throw new UnsupportedOperationException("Fabric configuration sync is unavailable");
    }
}
