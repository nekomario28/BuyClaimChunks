package me.skyadri.buyclaimchunks;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class InventoryPayment {
    private InventoryPayment() {
    }

    static long count(Inventory inventory, Item requiredItem) {
        long total = 0L;
        for (ItemStack stack : inventory.items) {
            if (stack.is(requiredItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    static boolean consume(Inventory inventory, Item requiredItem, long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Payment amount must be non-negative");
        }
        if (count(inventory, requiredItem) < amount) {
            return false;
        }

        long remaining = amount;
        for (ItemStack stack : inventory.items) {
            if (stack.is(requiredItem)) {
                int remove = (int) Math.min(stack.getCount(), remaining);
                stack.shrink(remove);
                remaining -= remove;
                if (remaining == 0L) {
                    return true;
                }
            }
        }

        return remaining == 0L;
    }
}
