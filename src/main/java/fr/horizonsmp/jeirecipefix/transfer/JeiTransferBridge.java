package fr.horizonsmp.jeirecipefix.transfer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JeiTransferBridge implements PluginMessageListener {
    private static final String DELETE = "jei:delete_player_item";
    private static final String GIVE = "jei:give_item_stack";
    private static final String TRANSFER = "jei:recipe_transfer";
    private static final String TRANSFER_COUNTED = "jei:recipe_transfer_counted";
    private static final String HOTBAR = "jei:set_hotbar_item_stack";
    private static final String CHEAT_REQUEST = "jei:request_cheat_permission";
    private static final List<String> CHANNELS = List.of(
            DELETE, GIVE, TRANSFER, TRANSFER_COUNTED, HOTBAR, CHEAT_REQUEST);

    private final JavaPlugin plugin;

    public JeiTransferBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        for (String channel : CHANNELS) {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        }
        plugin.getLogger().info("JEI Fabric recipe-transfer channels registered.");
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(TRANSFER) && !channel.equals(TRANSFER_COUNTED)) return;
        try {
            TransferPacket packet = TransferPacket.decode(message, channel.equals(TRANSFER_COUNTED));
            apply(player, packet);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().warning("Rejected invalid JEI transfer packet from " + player.getName()
                    + ": " + exception.getMessage());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("JEI transfer failed for " + player.getName()
                    + ": " + exception.getMessage());
        }
    }

    private static void apply(Player player, TransferPacket packet) {
        InventoryView view = player.getOpenInventory();
        int slotCount = view.countSlots();
        validateSlots(packet.craftingSlots(), slotCount);
        validateSlots(packet.inventorySlots(), slotCount);
        for (TransferPacket.Operation operation : packet.operations()) {
            validateSlot(operation.inventorySlot(), slotCount);
            validateSlot(operation.craftingSlot(), slotCount);
            if (!packet.inventorySlots().contains(operation.inventorySlot())
                    || !packet.craftingSlots().contains(operation.craftingSlot())) {
                throw new IllegalArgumentException("Operation references a slot outside its declared group");
            }
        }

        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int slot : packet.craftingSlots()) snapshot.put(slot, cloneOrNull(view.getItem(slot)));
        for (int slot : packet.inventorySlots()) snapshot.put(slot, cloneOrNull(view.getItem(slot)));

        try {
            clearCraftingGrid(player, view, packet.craftingSlots());
            int sets = packet.maxTransfer() ? maximumCompleteSets(view, packet.operations()) : 1;
            if (sets < 1) throw new IllegalArgumentException("Required ingredients are no longer available");
            moveOperations(view, packet.operations(), sets);
            player.updateInventory();
        } catch (RuntimeException exception) {
            snapshot.forEach(view::setItem);
            player.updateInventory();
            throw exception;
        }
    }

    private static void clearCraftingGrid(Player player, InventoryView view, List<Integer> craftingSlots) {
        for (int slot : craftingSlots) {
            ItemStack stack = view.getItem(slot);
            if (isEmpty(stack)) continue;
            view.setItem(slot, null);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private static int maximumCompleteSets(InventoryView view, List<TransferPacket.Operation> operations) {
        Map<Integer, Integer> requiredBySource = new HashMap<>();
        for (TransferPacket.Operation operation : operations) {
            requiredBySource.merge(operation.inventorySlot(), operation.count(), Integer::sum);
        }
        int sets = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : requiredBySource.entrySet()) {
            ItemStack source = view.getItem(entry.getKey());
            if (isEmpty(source)) return 0;
            sets = Math.min(sets, source.getAmount() / entry.getValue());
        }
        for (TransferPacket.Operation operation : operations) {
            ItemStack source = view.getItem(operation.inventorySlot());
            if (isEmpty(source)) return 0;
            sets = Math.min(sets, source.getMaxStackSize() / operation.count());
        }
        return sets == Integer.MAX_VALUE ? 0 : sets;
    }

    private static void moveOperations(InventoryView view, List<TransferPacket.Operation> operations, int sets) {
        for (TransferPacket.Operation operation : operations) {
            ItemStack source = view.getItem(operation.inventorySlot());
            if (isEmpty(source)) throw new IllegalArgumentException("Ingredient source slot is empty");
            int amount = Math.multiplyExact(operation.count(), sets);
            if (source.getAmount() < amount) throw new IllegalArgumentException("Ingredient count changed during transfer");

            ItemStack destination = view.getItem(operation.craftingSlot());
            if (!isEmpty(destination) && !destination.isSimilar(source)) {
                throw new IllegalArgumentException("Crafting destination contains another item");
            }
            int existing = isEmpty(destination) ? 0 : destination.getAmount();
            if (existing + amount > source.getMaxStackSize()) {
                throw new IllegalArgumentException("Crafting destination would overflow");
            }

            ItemStack placed = source.clone();
            placed.setAmount(existing + amount);
            view.setItem(operation.craftingSlot(), placed);
            source.setAmount(source.getAmount() - amount);
            view.setItem(operation.inventorySlot(), source.getAmount() == 0 ? null : source);
        }
    }

    private static void validateSlots(List<Integer> slots, int slotCount) {
        if (slots.size() > 256) throw new IllegalArgumentException("Too many slots");
        for (int slot : slots) validateSlot(slot, slotCount);
    }

    private static void validateSlot(int slot, int slotCount) {
        if (slot < 0 || slot >= slotCount) throw new IllegalArgumentException("Invalid slot: " + slot);
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() < 1;
    }

    private static ItemStack cloneOrNull(ItemStack stack) {
        return isEmpty(stack) ? null : stack.clone();
    }
}
