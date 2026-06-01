package fr.horizonsmp.jeirecipefix.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;

public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Plugin plugin;
    private YamlConfiguration messages;
    private String prefix;

    public Messages(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = messages.getString("prefix", "");
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        String raw = messages.getString(key, key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        Component component = LEGACY.deserialize(prefix + raw);
        target.sendMessage(component);
    }

    public void send(CommandSender target, String key) {
        send(target, key, Map.of());
    }
}
