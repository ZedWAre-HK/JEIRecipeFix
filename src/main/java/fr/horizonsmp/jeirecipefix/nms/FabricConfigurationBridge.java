package fr.horizonsmp.jeirecipefix.nms;

import fr.horizonsmp.jeirecipefix.sync.RecipeSyncService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class FabricConfigurationBridge {
    private static final String SUPPORTED_SERIALIZERS = "fabric:recipe_sync/supported_serializers";
    private static final String UPDATE_RECIPES_PACKET = "net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket";
    private static final AtomicLong HANDLER_IDS = new AtomicLong();

    private final Plugin plugin;
    private final RecipeBridge recipeBridge;
    private final RecipeSyncService syncService;
    private final Listener configurationListener = new Listener() {
    };
    private final Map<Object, Session> sessions = Collections.synchronizedMap(new IdentityHashMap<>());
    private PluginMessageListener messageListener;
    private volatile byte[] fabricPayload;
    private boolean configurationApiAvailable;

    public FabricConfigurationBridge(Plugin plugin, RecipeBridge recipeBridge, RecipeSyncService syncService) {
        this.plugin = plugin;
        this.recipeBridge = recipeBridge;
        this.syncService = syncService;
    }

    public void refreshFabricPayload() {
        fabricPayload = recipeBridge.isAvailable() ? syncService.fabricPayload() : null;
    }

    public void register() {
        messageListener = (PluginMessageListener) Proxy.newProxyInstance(
                PluginMessageListener.class.getClassLoader(),
                new Class<?>[] {PluginMessageListener.class},
                new MessageListenerInvocationHandler());
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, SUPPORTED_SERIALIZERS, messageListener);
        try {
            Class<?> eventType = Class.forName(
                    "io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent");
            plugin.getServer().getPluginManager().registerEvent(
                    eventType.asSubclass(Event.class), configurationListener, EventPriority.NORMAL,
                    (listener, event) -> onInitialConfiguration(event), plugin, false);
            configurationApiAvailable = true;
            plugin.getLogger().info("Fabric configuration-phase recipe sync is active.");
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("This server has no configuration-phase plugin messaging API; using join-time recipe sync.");
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(configurationListener);
        if (messageListener != null) {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, SUPPORTED_SERIALIZERS, messageListener);
        }
        synchronized (sessions) {
            sessions.values().forEach(Session::remove);
            sessions.clear();
        }
    }

    private void onInitialConfiguration(Event event) throws EventException {
        if (!configurationApiAvailable || fabricPayload == null) {
            return;
        }
        try {
            Object configurationConnection = event.getClass().getMethod("getConnection").invoke(event);
            Session session = new Session(configurationConnection, fabricPayload);
            sessions.put(configurationConnection, session);
            recipeBridge.advertiseFabricRecipeSync(configurationConnection);
            session.install();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Unable to prepare Fabric configuration recipe sync: " + exception.getMessage());
        }
    }

    private void onSupportedSerializers(Object connection) {
        Session session = sessions.get(connection);
        if (session != null) {
            session.confirmed = true;
        }
    }

    private final class MessageListenerInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "JEIRecipeFix configuration message listener";
                    default -> null;
                };
            }
            if (method.getName().equals("onPluginMessageReceived") && args != null && args.length == 3
                    && SUPPORTED_SERIALIZERS.equals(args[0])) {
                onSupportedSerializers(args[1]);
            }
            return null;
        }
    }

    private final class Session {
        private final Object configurationConnection;
        private final byte[] payload;
        private final String handlerName = "jeirecipefix-recipe-sync-" + HANDLER_IDS.incrementAndGet();
        private Channel channel;
        private volatile boolean confirmed;
        private volatile boolean injected;

        private Session(Object configurationConnection, byte[] payload) {
            this.configurationConnection = configurationConnection;
            this.payload = payload;
        }

        private void install() {
            Object handler = Reflect.getField(configurationConnection, "handle");
            Object connection = Reflect.getField(handler, "connection");
            channel = (Channel) Reflect.getField(connection, "channel");
            channel.eventLoop().execute(() -> {
                try {
                    if (channel.pipeline().get(handlerName) == null) {
                        channel.pipeline().addBefore("packet_handler", handlerName, new RecipePacketInterceptor());
                    }
                } catch (RuntimeException exception) {
                    sessions.remove(configurationConnection);
                    plugin.getLogger().warning("Unable to install Fabric recipe sync interceptor: " + exception.getMessage());
                }
            });
        }

        private void remove() {
            if (channel != null) {
                channel.eventLoop().execute(() -> {
                    if (channel.pipeline().get(handlerName) != null) {
                        channel.pipeline().remove(handlerName);
                    }
                });
            }
        }

        private final class RecipePacketInterceptor extends ChannelOutboundHandlerAdapter {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                if (!injected && confirmed && UPDATE_RECIPES_PACKET.equals(message.getClass().getName())) {
                    injected = true;
                    try {
                        context.write(recipeBridge.createFabricPacket(payload));
                    } catch (RuntimeException exception) {
                        plugin.getLogger().warning("Unable to inject Fabric recipe sync: " + exception.getMessage());
                    }
                }
                context.write(message, promise);
            }
        }
    }
}
