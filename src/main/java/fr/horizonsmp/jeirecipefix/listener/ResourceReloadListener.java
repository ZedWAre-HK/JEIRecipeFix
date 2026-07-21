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
    private final Runnable configurationPayloadRefresh;

    public ResourceReloadListener(RecipeSyncService syncService, Supplier<PluginConfig> config,
                                  Runnable configurationPayloadRefresh) {
        this.syncService = syncService;
        this.config = config;
        this.configurationPayloadRefresh = configurationPayloadRefresh;
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        syncService.invalidate();
        configurationPayloadRefresh.run();
        if (config.get().syncOnDatapackReload()) {
            syncService.resyncAll();
        }
    }
}
