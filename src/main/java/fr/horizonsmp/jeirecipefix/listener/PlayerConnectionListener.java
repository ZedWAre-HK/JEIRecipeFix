package fr.horizonsmp.jeirecipefix.listener;

import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class PlayerConnectionListener implements Listener {

    private final Plugin plugin;
    private final RecipeSyncService syncService;
    private final Supplier<PluginConfig> config;
    private final Predicate<UUID> earlyFabricSync;

    public PlayerConnectionListener(Plugin plugin, RecipeSyncService syncService, Supplier<PluginConfig> config,
                                    Predicate<UUID> earlyFabricSync) {
        this.plugin = plugin;
        this.syncService = syncService;
        this.config = config;
        this.earlyFabricSync = earlyFabricSync;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.get().syncOnJoin()) {
            return;
        }
        Player player = event.getPlayer();
        if (earlyFabricSync.test(player.getUniqueId())) {
            return;
        }
        boolean synced = syncService.syncTo(player);
        if (!synced) {
            player.getScheduler().runDelayed(plugin, task -> syncService.syncTo(player), null, 20L);
        }
    }
}
