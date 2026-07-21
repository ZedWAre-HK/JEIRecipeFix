package fr.horizonsmp.jeirecipefix;

import fr.horizonsmp.jeirecipefix.command.JEIRecipeFixCommand;
import fr.horizonsmp.jeirecipefix.config.ConfigLoader;
import fr.horizonsmp.jeirecipefix.config.PluginConfig;
import fr.horizonsmp.jeirecipefix.i18n.Messages;
import fr.horizonsmp.jeirecipefix.listener.PlayerConnectionListener;
import fr.horizonsmp.jeirecipefix.listener.ResourceReloadListener;
import fr.horizonsmp.jeirecipefix.transfer.JeiTransferBridge;
import fr.horizonsmp.jeirecipefix.nms.FabricConfigurationBridge;
import fr.horizonsmp.jeirecipefix.nms.NmsRecipeBridge;
import fr.horizonsmp.jeirecipefix.nms.RecipeBridge;
import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicReference;

public final class JEIRecipeFix extends JavaPlugin {

    private final AtomicReference<PluginConfig> config = new AtomicReference<>(PluginConfig.defaults());
    private RecipeSyncService syncService;
    private Messages messages;
    private JeiTransferBridge transferBridge;
    private FabricConfigurationBridge configurationBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        RecipeBridge bridge = new NmsRecipeBridge(this);
        if (!bridge.isAvailable()) {
            getLogger().warning("Unsupported server internals; recipe sync disabled. JEIRecipeFix will stay dormant.");
        }
        this.syncService = new RecipeSyncService(bridge, config::get, this, getLogger());

        this.configurationBridge = new FabricConfigurationBridge(this, bridge, syncService);
        configurationBridge.refreshFabricPayload();
        configurationBridge.register();

        this.transferBridge = new JeiTransferBridge(this);
        transferBridge.register();

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, syncService, config::get,
                        configurationBridge::consumeEarlyFabricSync), this);
        getServer().getPluginManager().registerEvents(
                new ResourceReloadListener(syncService, config::get, configurationBridge::refreshFabricPayload), this);

        this.messages = new Messages(this);
        JEIRecipeFixCommand command = new JEIRecipeFixCommand(this, syncService, messages);
        getCommand("jeirecipefix").setExecutor(command);
        getCommand("jeirecipefix").setTabCompleter(command);

        getLogger().info("JEIRecipeFix enabled (recipe sync "
                + (bridge.isAvailable() ? "active" : "dormant") + ").");
    }

    @Override
    public void onDisable() {
        if (configurationBridge != null) {
            configurationBridge.unregister();
        }
        if (transferBridge != null) {
            transferBridge.unregister();
        }
    }

    public void reloadAll() {
        reloadConfig();
        reloadPluginConfig();
        if (messages != null) messages.reload();
        if (syncService != null) {
            syncService.invalidate();
        }
        if (configurationBridge != null) {
            configurationBridge.refreshFabricPayload();
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
