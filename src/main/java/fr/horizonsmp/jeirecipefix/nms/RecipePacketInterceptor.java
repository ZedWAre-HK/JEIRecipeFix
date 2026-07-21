package fr.horizonsmp.jeirecipefix.nms;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class RecipePacketInterceptor extends ChannelOutboundHandlerAdapter {
    private static final String UPDATE_RECIPES_PACKET = "net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket";

    private final BooleanSupplier confirmed;
    private final Supplier<Object> recipePacket;
    private final Runnable injected;
    private final Runnable finished;
    private final Consumer<RuntimeException> failure;

    RecipePacketInterceptor(BooleanSupplier confirmed, Supplier<Object> recipePacket, Runnable injected,
                            Runnable finished, Consumer<RuntimeException> failure) {
        this.confirmed = confirmed;
        this.recipePacket = recipePacket;
        this.injected = injected;
        this.finished = finished;
        this.failure = failure;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (UPDATE_RECIPES_PACKET.equals(message.getClass().getName())) {
            if (confirmed.getAsBoolean()) {
                try {
                    context.write(recipePacket.get());
                    injected.run();
                } catch (RuntimeException exception) {
                    failure.accept(exception);
                }
            }
            finished.run();
        }
        context.write(message, promise);
    }
}
