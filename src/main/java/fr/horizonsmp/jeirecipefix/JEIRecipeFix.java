package fr.horizonsmp.jeirecipefix;

import fr.horizonsmp.jeirecipefix.command.JEIRecipeFixCommand;
import fr.horizonsmp.jeirecipefix.config.ConfigLoader;
import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.i18n.Messages;
import fr.horizonsmp.jeirecipefix.listener.PlayerConnectionListener;
import fr.horizonsmp.jeirecipefix.listener.ResourceReloadListener;
import fr.horizonsmp.jeirecipefix.nms.NmsRecipeBridge;
import fr.horizonsmp.jeirecipefix.nms.RecipeBridge;
import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicReference;

public final class JEIRecipeFix extends JavaPlugin {

    private final AtomicReference<PluginConfig> config = new AtomicReference<>(PluginConfig.defaults());
    private RecipeSyncService syncService;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        RecipeBridge bridge = new NmsRecipeBridge(this);
        if (!bridge.isAvailable()) {
            getLogger().warning("Unsupported server internals; recipe sync disabled. JEIRecipeFix will stay dormant.");
        }
        this.syncService = new RecipeSyncService(bridge, config::get, this, getLogger());

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, syncService, config::get), this);
        getServer().getPluginManager().registerEvents(
                new ResourceReloadListener(syncService, config::get), this);

        this.messages = new Messages(this);
        JEIRecipeFixCommand command = new JEIRecipeFixCommand(this, syncService, messages);
        getCommand("jeirecipefix").setExecutor(command);
        getCommand("jeirecipefix").setTabCompleter(command);

        getLogger().info("JEIRecipeFix enabled (recipe sync "
                + (bridge.isAvailable() ? "active" : "dormant") + ").");
    }

    public void reloadAll() {
        reloadConfig();
        reloadPluginConfig();
        if (messages != null) messages.reload();
        if (syncService != null) {
            syncService.invalidate();
        }
    }

    public RecipeSyncService syncService() {
        return syncService;
    }

    public PluginConfig pluginConfig() {
        return config.get();
    }

    private void reloadPluginConfig() {
        config.set(ConfigLoader.fromSection(getConfig()));
    }
}
