package fr.horizonsmp.jeirecipefix.listener;

import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Supplier;

public final class ResourceReloadListener implements Listener {

    private final RecipeSyncService syncService;
    private final Supplier<PluginConfig> config;

    public ResourceReloadListener(RecipeSyncService syncService, Supplier<PluginConfig> config) {
        this.syncService = syncService;
        this.config = config;
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        syncService.invalidate();
        if (config.get().syncOnDatapackReload()) {
            syncService.resyncAll();
        }
    }
}
