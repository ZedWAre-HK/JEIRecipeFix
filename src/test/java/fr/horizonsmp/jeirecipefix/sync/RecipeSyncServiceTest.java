package fr.horizonsmp.jeirecipefix.sync;

import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.nms.RecipeBridge;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeSyncServiceTest {

    private static final byte[] FABRIC_BYTES = {1, 2, 3};

    private final AtomicInteger fabricBuilds = new AtomicInteger();

    private RecipeBridge bridge(boolean available) {
        return new RecipeBridge() {
            @Override public boolean isAvailable() { return available; }
            @Override public int recipeCount() { return 7; }
            @Override public byte[] buildFabricPayload() { fabricBuilds.incrementAndGet(); return FABRIC_BYTES; }
            @Override public byte[] buildNeoForgePayload() { return new byte[]{9}; }
            @Override public void sendFabric(Player player, byte[] payload) { }
            @Override public void sendNeoForge(Player player, byte[] payload) { }
        };
    }

    private RecipeSyncService service(boolean available, PluginConfig config) {
        return new RecipeSyncService(bridge(available), () -> config, null, Logger.getAnonymousLogger());
    }

    @Test
    void shouldSyncOnlyWhenEnabledAvailableAndSupportedBrand() {
        RecipeSyncService enabled = service(true, PluginConfig.defaults());
        assertTrue(enabled.shouldSync(ClientBrand.FABRIC));
        assertTrue(enabled.shouldSync(ClientBrand.NEOFORGE));
        assertFalse(enabled.shouldSync(ClientBrand.OTHER));

        RecipeSyncService disabled = service(true, new PluginConfig(false, true, true, false));
        assertFalse(disabled.shouldSync(ClientBrand.FABRIC));

        RecipeSyncService unavailable = service(false, PluginConfig.defaults());
        assertFalse(unavailable.shouldSync(ClientBrand.FABRIC));
    }

    @Test
    void cachesPayloadUntilInvalidated() {
        RecipeSyncService service = service(true, PluginConfig.defaults());

        assertArrayEquals(FABRIC_BYTES, service.payloadFor(ClientBrand.FABRIC));
        assertArrayEquals(FABRIC_BYTES, service.payloadFor(ClientBrand.FABRIC));
        assertEquals(1, fabricBuilds.get());

        service.invalidate();
        assertArrayEquals(FABRIC_BYTES, service.payloadFor(ClientBrand.FABRIC));
        assertEquals(2, fabricBuilds.get());
    }
}
