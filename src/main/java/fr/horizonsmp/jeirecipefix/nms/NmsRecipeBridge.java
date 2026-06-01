package fr.horizonsmp.jeirecipefix.nms;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class NmsRecipeBridge implements RecipeBridge {

    private static final String FABRIC_CHANNEL = "fabric:recipe_sync";
    private static final String NEOFORGE_CHANNEL = "neoforge:recipe_content";

    private final Plugin plugin;
    private boolean available;

    private Object minecraftServer;
    private Object registryAccess;
    private Object serializerRegistry;
    private Method registryGetKey;
    private Method getRecipes;
    private Method holderId;
    private Method holderValue;
    private Method recipeGetSerializer;
    private Method serializerStreamCodec;
    private Method streamCodecEncode;
    private Method bufWriteVarInt;
    private Method bufWriteResourceLocation;
    private Method resourceKeyLocation;
    private Constructor<?> registryBufCtor;
    private Object recipesCache;
    private int recipeCount;

    private Object recipeTypeRegistry;       // BuiltInRegistries.RECIPE_TYPE
    private Method recipeGetType;            // Recipe#getType()
    private Method recipeTypeRegistryGetId;  // Registry#getId(Object) -> int
    private Object recipeHolderStreamCodec;  // RecipeHolder.STREAM_CODEC (static field)
    private Object serverRegistries;         // MinecraftServer#registries() -> LayeredRegistryAccess<RegistryLayer>
    private Method serializeTagsToNetwork;   // TagNetworkSerialization#serializeTagsToNetwork(LayeredRegistryAccess) -> Map
    private Constructor<?> updateTagsPacketCtor; // ClientboundUpdateTagsPacket(Map)

    private Method getHandle;
    private Method connectionSend;
    private Constructor<?> customPayloadPacketCtor;
    private Constructor<?> discardedPayloadCtor;
    private Object fabricPayloadId;
    private Object neoForgePayloadId;
    private boolean sendFailureLogged;

    public NmsRecipeBridge(Plugin plugin) {
        this.plugin = plugin;
        try {
            resolveHandles();
            this.available = true;
        } catch (RuntimeException e) {
            this.available = false;
            plugin.getLogger().warning("Could not resolve server internals: " + e.getMessage());
        }
    }

    private void resolveHandles() {
        Object craftServer = org.bukkit.Bukkit.getServer();
        Method getServer = Reflect.method(craftServer.getClass(), "getServer");
        this.minecraftServer = Reflect.call(getServer, craftServer);

        Method registryAccessMethod = Reflect.method(minecraftServer.getClass(), "registryAccess");
        this.registryAccess = Reflect.call(registryAccessMethod, minecraftServer);

        Method getRecipeManager = Reflect.method(minecraftServer.getClass(), "getRecipeManager");
        Object recipeManager = Reflect.call(getRecipeManager, minecraftServer);
        this.getRecipes = Reflect.method(recipeManager.getClass(), "getRecipes");
        Collection<?> recipes = (Collection<?>) Reflect.call(getRecipes, recipeManager);
        this.recipesCache = recipes;
        this.recipeCount = recipes.size();

        Class<?> builtInRegistries = Reflect.clazz("net.minecraft.core.registries.BuiltInRegistries");
        this.serializerRegistry = Reflect.staticField(builtInRegistries, "RECIPE_SERIALIZER");
        this.registryGetKey = Reflect.method(serializerRegistry.getClass(), "getKey", Object.class);

        // Resolve holder/recipe/serializer/codec handles from their declaring types (not a sample
        // concrete instance) so the cached Method handles dispatch virtually across every
        // implementation and no sample recipe is needed (an empty recipe set must not throw).
        Class<?> recipeHolder = Reflect.clazz("net.minecraft.world.item.crafting.RecipeHolder");
        this.holderId = Reflect.method(recipeHolder, "id");
        this.holderValue = Reflect.method(recipeHolder, "value");

        Class<?> recipeClass = Reflect.clazz("net.minecraft.world.item.crafting.Recipe");
        this.recipeGetSerializer = Reflect.method(recipeClass, "getSerializer");
        Class<?> recipeSerializerClass = Reflect.clazz("net.minecraft.world.item.crafting.RecipeSerializer");
        this.serializerStreamCodec = Reflect.method(recipeSerializerClass, "streamCodec");
        Class<?> streamEncoderClass = Reflect.clazz("net.minecraft.network.codec.StreamEncoder");
        this.streamCodecEncode = Reflect.method(streamEncoderClass, "encode", Object.class, Object.class);

        // NeoForge path: recipe types written as a registry collection, holders via RecipeHolder.STREAM_CODEC.
        this.recipeTypeRegistry = Reflect.staticField(builtInRegistries, "RECIPE_TYPE");
        this.recipeTypeRegistryGetId = Reflect.method(recipeTypeRegistry.getClass(), "getId", Object.class);
        this.recipeGetType = Reflect.method(recipeClass, "getType");
        // RecipeHolder.STREAM_CODEC is a StreamCodec (which extends StreamEncoder), so streamCodecEncode applies to it.
        this.recipeHolderStreamCodec = Reflect.staticField(recipeHolder, "STREAM_CODEC");

        // NeoForge clients also expect tags: build a vanilla ClientboundUpdateTagsPacket from the server's frozen
        // registries. serializeTagsToNetwork(LayeredRegistryAccess) yields exactly the Map the packet constructor needs.
        Method serverRegistriesMethod = Reflect.method(minecraftServer.getClass(), "registries");
        this.serverRegistries = Reflect.call(serverRegistriesMethod, minecraftServer);
        Class<?> tagNetworkSerialization = Reflect.clazz("net.minecraft.tags.TagNetworkSerialization");
        Class<?> layeredRegistryAccess = Reflect.clazz("net.minecraft.core.LayeredRegistryAccess");
        this.serializeTagsToNetwork = Reflect.method(tagNetworkSerialization, "serializeTagsToNetwork", layeredRegistryAccess);
        Class<?> updateTagsPacket = Reflect.clazz("net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket");
        this.updateTagsPacketCtor = Reflect.ctor(updateTagsPacket, Map.class);

        Class<?> friendlyByteBuf = Reflect.clazz("net.minecraft.network.FriendlyByteBuf");
        this.bufWriteVarInt = Reflect.method(friendlyByteBuf, "writeVarInt", int.class);
        Class<?> resourceLocation = Reflect.clazz("net.minecraft.resources.ResourceLocation");
        this.bufWriteResourceLocation = Reflect.method(friendlyByteBuf, "writeResourceLocation", resourceLocation);
        Class<?> resourceKey = Reflect.clazz("net.minecraft.resources.ResourceKey");
        this.resourceKeyLocation = Reflect.method(resourceKey, "location");

        Class<?> registryBuf = Reflect.clazz("net.minecraft.network.RegistryFriendlyByteBuf");
        Class<?> registryAccessClass = Reflect.clazz("net.minecraft.core.RegistryAccess");
        this.registryBufCtor = Reflect.ctor(registryBuf, ByteBuf.class, registryAccessClass);

        Class<?> customPayloadPacket = Reflect.clazz("net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket");
        Class<?> customPacketPayload = Reflect.clazz("net.minecraft.network.protocol.common.custom.CustomPacketPayload");
        this.customPayloadPacketCtor = Reflect.ctor(customPayloadPacket, customPacketPayload);
        Class<?> discardedPayload = Reflect.clazz("net.minecraft.network.protocol.common.custom.DiscardedPayload");
        // DiscardedPayload(ResourceLocation id, byte[] data): carries raw bytes for a channel the server has no codec for.
        this.discardedPayloadCtor = Reflect.ctor(discardedPayload, resourceLocation, byte[].class);
        this.fabricPayloadId = newResourceLocation(resourceLocation, FABRIC_CHANNEL);
        this.neoForgePayloadId = newResourceLocation(resourceLocation, NEOFORGE_CHANNEL);

        Class<?> craftPlayer = Reflect.clazz("org.bukkit.craftbukkit.entity.CraftPlayer");
        this.getHandle = Reflect.method(craftPlayer, "getHandle");
    }

    private Object newResourceLocation(Class<?> resourceLocation, String id) {
        try {
            // ResourceLocation.parse(String): the 1.21 static factory (the old 2-arg constructor was removed).
            Method parse = resourceLocation.getMethod("parse", String.class);
            return parse.invoke(null, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build ResourceLocation " + id, e);
        }
    }

    private Object newRegistryBuf() {
        try {
            return registryBufCtor.newInstance(Unpooled.buffer(), registryAccess);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create RegistryFriendlyByteBuf", e);
        }
    }

    private static byte[] toBytes(Object friendlyByteBuf) {
        ByteBuf buf = (ByteBuf) friendlyByteBuf;
        byte[] out = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), out);
        return out;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int recipeCount() {
        return recipeCount;
    }

    @Override
    public byte[] buildFabricPayload() {
        Map<Object, List<Object>> bySerializer = new LinkedHashMap<>();
        for (Object holder : (Collection<?>) recipesCache) {
            Object recipe = Reflect.call(holderValue, holder);
            Object serializer = Reflect.call(recipeGetSerializer, recipe);
            bySerializer.computeIfAbsent(serializer, s -> new ArrayList<>()).add(holder);
        }

        Object buf = newRegistryBuf();
        try {
            Reflect.call(bufWriteVarInt, buf, bySerializer.size());
            for (Map.Entry<Object, List<Object>> entry : bySerializer.entrySet()) {
                Object serializer = entry.getKey();
                List<Object> holders = entry.getValue();
                Object serializerId = Reflect.call(registryGetKey, serializerRegistry, serializer);
                Reflect.call(bufWriteResourceLocation, buf, serializerId);
                Reflect.call(bufWriteVarInt, buf, holders.size());
                Object codec = Reflect.call(serializerStreamCodec, serializer);
                for (Object holder : holders) {
                    Object id = Reflect.call(holderId, holder);
                    Object location = Reflect.call(resourceKeyLocation, id);
                    Reflect.call(bufWriteResourceLocation, buf, location);
                    Object recipe = Reflect.call(holderValue, holder);
                    Reflect.call(streamCodecEncode, codec, buf, recipe);
                }
            }
            return toBytes(buf);
        } finally {
            ((io.netty.buffer.ByteBuf) buf).release();
        }
    }

    @Override
    public byte[] buildNeoForgePayload() {
        LinkedHashSet<Object> types = new LinkedHashSet<>();
        List<Object> holders = new ArrayList<>();
        for (Object holder : (Collection<?>) recipesCache) {
            holders.add(holder);
            Object recipe = Reflect.call(holderValue, holder);
            types.add(Reflect.call(recipeGetType, recipe));
        }

        Object buf = newRegistryBuf();
        try {
            Reflect.call(bufWriteVarInt, buf, types.size());
            for (Object type : types) {
                int id = (int) Reflect.call(recipeTypeRegistryGetId, recipeTypeRegistry, type);
                Reflect.call(bufWriteVarInt, buf, id);
            }
            Reflect.call(bufWriteVarInt, buf, holders.size());
            for (Object holder : holders) {
                Reflect.call(streamCodecEncode, recipeHolderStreamCodec, buf, holder);
            }
            return toBytes(buf);
        } finally {
            ((io.netty.buffer.ByteBuf) buf).release();
        }
    }

    @Override
    public void sendFabric(Player player, byte[] payload) {
        send(player, fabricPayloadId, payload);
    }

    @Override
    public void sendNeoForge(Player player, byte[] payload) {
        send(player, neoForgePayloadId, payload);
        sendTagsPacket(player);
    }

    /** Builds the vanilla tags packet from the server's frozen registries (no player needed; unit-testable). */
    Object buildTagsPacket() {
        try {
            Object tags = Reflect.call(serializeTagsToNetwork, null, serverRegistries);
            return updateTagsPacketCtor.newInstance(tags);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build ClientboundUpdateTagsPacket", e);
        }
    }

    private void sendTagsPacket(Player player) {
        try {
            Object serverPlayer = Reflect.call(getHandle, player);
            Object connection = Reflect.getField(serverPlayer, "connection");
            sendPacket(connection, buildTagsPacket());
        } catch (RuntimeException e) {
            if (!sendFailureLogged) {
                sendFailureLogged = true;
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Failed to send tags packet to " + player.getName() + "; suppressing further send errors.", e);
            }
        }
    }

    private void send(Player player, Object payloadId, byte[] payload) {
        try {
            Object serverPlayer = Reflect.call(getHandle, player);
            // ServerPlayer.connection (Mojang-mapped, stable on the Paper family).
            Object connection = Reflect.getField(serverPlayer, "connection");
            Object discarded = discardedPayloadCtor.newInstance(payloadId, payload);
            Object packet = customPayloadPacketCtor.newInstance(discarded);
            sendPacket(connection, packet);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!sendFailureLogged) {
                sendFailureLogged = true;
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Failed to send recipe payload to " + player.getName() + "; suppressing further send errors.", e);
            }
        }
    }

    /** Sends a built packet over the player's connection, lazily resolving the cached send handle. */
    private void sendPacket(Object connection, Object packet) {
        if (connectionSend == null) {
            connectionSend = Reflect.method(connection.getClass(), "send",
                    Reflect.clazz("net.minecraft.network.protocol.Packet"));
        }
        Reflect.call(connectionSend, connection, packet);
    }
}
