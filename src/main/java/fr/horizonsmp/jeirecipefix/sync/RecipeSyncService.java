package fr.horizonsmp.jeirecipefix.sync;

import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.nms.RecipeBridge;
import fr.horizonsmp.jeirecipefix.util.Lazy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;
import java.util.logging.Logger;

public final class RecipeSyncService {

    private final RecipeBridge bridge;
    private final Supplier<PluginConfig> config;
    private final Plugin plugin;
    private final Logger logger;
    private final Lazy<byte[]> fabricPayload;
    private final Lazy<byte[]> neoForgePayload;

    public RecipeSyncService(RecipeBridge bridge, Supplier<PluginConfig> config, Plugin plugin, Logger logger) {
        this.bridge = bridge;
        this.config = config;
        this.plugin = plugin;
        this.logger = logger;
        this.fabricPayload = new Lazy<>(bridge::buildFabricPayload);
        this.neoForgePayload = new Lazy<>(bridge::buildNeoForgePayload);
    }

    public boolean shouldSync(ClientBrand brand) {
        return config.get().enabled() && bridge.isAvailable() && brand.isSupported();
    }

    public byte[] payloadFor(ClientBrand brand) {
        return switch (brand) {
            case FABRIC -> fabricPayload.get();
            case NEOFORGE -> neoForgePayload.get();
            default -> null;
        };
    }

    /** Sends recipes to one player if applicable. Returns true if a payload was sent. */
    public boolean syncTo(Player player) {
        ClientBrand brand = ClientBrand.fromBrand(player.getClientBrandName());
        if (!shouldSync(brand)) {
            if (config.get().debug()) {
                logger.info("Skipping recipe sync for " + player.getName() + " (brand=" + brand + ")");
            }
            return false;
        }
        byte[] payload = payloadFor(brand);
        if (payload == null) {
            return false;
        }
        switch (brand) {
            case FABRIC -> bridge.sendFabric(player, payload);
            case NEOFORGE -> bridge.sendNeoForge(player, payload);
            default -> {
                return false;
            }
        }
        if (config.get().debug()) {
            logger.info("Synced " + bridge.recipeCount() + " recipes to " + player.getName() + " (brand=" + brand + ")");
        }
        return true;
    }

    public void resyncAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, task -> syncTo(player), null);
        }
    }

    public void invalidate() {
        fabricPayload.invalidate();
        neoForgePayload.invalidate();
    }

    public boolean available() {
        return bridge.isAvailable();
    }

    public int recipeCount() {
        return bridge.recipeCount();
    }
}
