package fr.horizonsmp.jeirecipefix.listener;

import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public final class PlayerConnectionListener implements Listener {

    private final Plugin plugin;
    private final RecipeSyncService syncService;
    private final Supplier<PluginConfig> config;

    public PlayerConnectionListener(Plugin plugin, RecipeSyncService syncService, Supplier<PluginConfig> config) {
        this.plugin = plugin;
        this.syncService = syncService;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.get().syncOnJoin()) {
            return;
        }
        Player player = event.getPlayer();
        boolean synced = syncService.syncTo(player);
        if (!synced) {
            player.getScheduler().runDelayed(plugin, task -> syncService.syncTo(player), null, 20L);
        }
    }
}
