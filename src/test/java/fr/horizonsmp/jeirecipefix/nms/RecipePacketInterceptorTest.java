package fr.horizonsmp.jeirecipefix.nms;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipePacketInterceptorTest {
    @Test
    void injectsBeforeVanillaRecipesAfterConfirmation() {
        AtomicInteger injected = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        EmbeddedChannel channel = new EmbeddedChannel(new RecipePacketInterceptor(
                () -> true, () -> "fabric-recipes", injected::incrementAndGet,
                finished::incrementAndGet, exception -> { throw exception; }));
        ClientboundUpdateRecipesPacket vanillaPacket = new ClientboundUpdateRecipesPacket();

        assertTrue(channel.writeOutbound(vanillaPacket));
        assertEquals("fabric-recipes", channel.readOutbound());
        assertSame(vanillaPacket, channel.readOutbound());
        assertEquals(1, injected.get());
        assertEquals(1, finished.get());
        assertFalse(channel.finish());
    }

    @Test
    void leavesVanillaRecipesAloneWithoutConfirmation() {
        AtomicInteger injected = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        EmbeddedChannel channel = new EmbeddedChannel(new RecipePacketInterceptor(
                () -> false, () -> "fabric-recipes", injected::incrementAndGet,
                finished::incrementAndGet, exception -> { throw exception; }));
        ClientboundUpdateRecipesPacket vanillaPacket = new ClientboundUpdateRecipesPacket();

        assertTrue(channel.writeOutbound(vanillaPacket));
        assertSame(vanillaPacket, channel.readOutbound());
        assertEquals(0, injected.get());
        assertEquals(1, finished.get());
        assertFalse(channel.finish());
    }

    @Test
    void preservesVanillaRecipesWhenInjectionFails() {
        AtomicBoolean failed = new AtomicBoolean();
        AtomicInteger injected = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        EmbeddedChannel channel = new EmbeddedChannel(new RecipePacketInterceptor(
                () -> true, () -> { throw new IllegalStateException("broken"); }, injected::incrementAndGet,
                finished::incrementAndGet, exception -> failed.set(true)));
        ClientboundUpdateRecipesPacket vanillaPacket = new ClientboundUpdateRecipesPacket();

        assertTrue(channel.writeOutbound(vanillaPacket));
        assertSame(vanillaPacket, channel.readOutbound());
        assertTrue(failed.get());
        assertEquals(0, injected.get());
        assertEquals(1, finished.get());
        assertFalse(channel.finish());
    }
}
