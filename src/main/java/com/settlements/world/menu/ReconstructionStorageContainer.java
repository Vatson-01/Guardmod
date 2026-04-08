package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionSession;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ReconstructionStorageContainer extends SimpleContainer {
    private final ReconstructionSession session;
    private final SettlementSavedData data;

    public ReconstructionStorageContainer(ReconstructionSession session, SettlementSavedData data) {
        super(session.getStorageSize());
        this.session = session;
        this.data = data;

        NonNullList<ItemStack> storedItems = session.copyStoredItems();
        for (int i = 0; i < storedItems.size(); i++) {
            setItem(i, storedItems.get(i));
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        syncToSession();
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        syncToSession();
    }

    private void syncToSession() {
        NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) {
            items.set(i, getItem(i).copy());
        }

        session.overwriteStoredItems(items);
        data.addOrUpdateReconstruction(session);
    }
}