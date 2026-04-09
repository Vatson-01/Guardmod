package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.world.menu.ReconstructionStorageContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ReconstructionService {
    private ReconstructionService() {
    }

    public static ReconstructionSession getActiveReconstructionForPlayer(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlement.getId());
        if (session == null) {
            throw new IllegalStateException("У поселения нет активной реконструкции.");
        }

        return session;
    }

    public static void openStorage(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement playerSettlement = data.getSettlementByPlayer(player.getUUID());
        if (playerSettlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        Settlement currentSettlement = data.getSettlementByChunk(player.level(), new ChunkPos(player.blockPosition()));
        if (currentSettlement == null || !currentSettlement.getId().equals(playerSettlement.getId())) {
            throw new IllegalStateException("Склад реконструкции можно открыть только на территории своего поселения.");
        }

        ReconstructionSession session = data.getActiveReconstructionForSettlement(playerSettlement.getId());
        if (session == null) {
            throw new IllegalStateException("У поселения нет активной реконструкции.");
        }

        ReconstructionStorageContainer container = new ReconstructionStorageContainer(session, data);
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignoredPlayer) -> ChestMenu.sixRows(containerId, playerInventory, container),
                Component.literal("Склад реконструкции")
        ));
    }


    public static void stopActive(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }
        if (!settlement.isLeader(player.getUUID())) {
            throw new IllegalStateException("Только глава поселения может принудительно остановить реконструкцию.");
        }

        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlement.getId());
        if (session == null) {
            throw new IllegalStateException("У поселения нет активной реконструкции.");
        }

        session.setActive(false);
        data.addOrUpdateReconstruction(session);
    }

    public static int depositMainHand(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            throw new IllegalStateException("В главной руке нет предмета для внесения.");
        }

        int inserted = insertIntoStorage(session, stack.copy());
        if (inserted <= 0) {
            throw new IllegalStateException("В складе реконструкции нет места для этого предмета.");
        }

        stack.shrink(inserted);
        SettlementSavedData.get(player.server).addOrUpdateReconstruction(session);
        return inserted;
    }

    public static ReconstructionRestoreResult restoreAvailable(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);

        ReconstructionRestoreResult result = new ReconstructionRestoreResult();

        boolean progress;
        do {
            progress = false;

            List<ReconstructionBlockEntry> pending = new ArrayList<ReconstructionBlockEntry>();
            for (ReconstructionBlockEntry entry : session.getEntries()) {
                if (entry.isPending()) {
                    pending.add(entry);
                }
            }

            pending.sort(Comparator
                    .comparingInt((ReconstructionBlockEntry entry) -> entry.getPos().getY())
                    .thenComparingInt(entry -> entry.getPos().getX())
                    .thenComparingInt(entry -> entry.getPos().getZ()));

            for (ReconstructionBlockEntry entry : pending) {
                if (tryRestoreEntry(player, session, entry)) {
                    result.addRestored();
                    progress = true;
                }
            }
        } while (progress);

        for (ReconstructionBlockEntry entry : session.getEntries()) {
            if (!entry.isPending()) {
                continue;
            }

            ServerLevel level = player.server.getLevel(entry.getDimensionKey());
            if (level == null) {
                result.addOtherBlocked();
                continue;
            }

            BlockPos pos = entry.getPos();
            if (!canReplaceAt(level, pos)) {
                result.addOccupied();
                continue;
            }

            if (entry.getRequiredCount() > 0
                    && !entry.getRequiredItemId().isEmpty()
                    && session.getStoredResourceAmount(entry.getRequiredItemId()) < entry.getRequiredCount()) {
                result.addMissingResources();
                continue;
            }

            if (!entry.readOriginalState().canSurvive(level, pos)) {
                result.addBlockedBySupport();
                continue;
            }

            result.addOtherBlocked();
        }

        result.setRemainingPending(session.countPendingEntries());

        if (session.countPendingEntries() <= 0) {
            session.setActive(false);
        }

        data.addOrUpdateReconstruction(session);
        return result;
    }

    public static void skipLookedAtBlock(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);

        HitResult hitResult = player.pick(8.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult)) {
            throw new IllegalStateException("Нужно смотреть на блок, который требуется для реконструкции.");
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
        String targetDimension = player.level().dimension().location().toString();

        ReconstructionBlockEntry found = null;
        for (ReconstructionBlockEntry entry : session.getEntries()) {
            if (!entry.isPending()) {
                continue;
            }

            if (!entry.getDimensionId().toString().equals(targetDimension)) {
                continue;
            }

            if (entry.getPos().equals(targetPos)) {
                found = entry;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("Этот блок не найден среди ожидающих восстановления позиций.");
        }

        found.setSkipped(true);

        if (session.countPendingEntries() <= 0) {
            session.setActive(false);
        }

        data.addOrUpdateReconstruction(session);
    }

    public static void skipEntryByIndex(ServerPlayer player, int index) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);

        if (index <= 0 || index > session.getEntries().size()) {
            throw new IllegalStateException("Неверный индекс записи реконструкции.");
        }

        ReconstructionBlockEntry entry = session.getEntries().get(index - 1);
        if (entry.isRestored()) {
            throw new IllegalStateException("Эта запись уже восстановлена.");
        }

        boolean newSkipped = !entry.isSkipped();
        entry.setSkipped(newSkipped);

        if (!newSkipped) {
            session.setActive(true);
        } else if (session.countPendingEntries() <= 0) {
            session.setActive(false);
        }

        data.addOrUpdateReconstruction(session);
    }

    public static String buildShortResourceSummary(ReconstructionSession session) {
        Map<String, Integer> resources = session.getStoredResources();
        if (resources.isEmpty()) {
            return "пусто";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append(" x").append(entry.getValue());
            first = false;
        }

        return builder.toString();
    }

    private static boolean tryRestoreEntry(ServerPlayer player, ReconstructionSession session, ReconstructionBlockEntry entry) {
        ServerLevel level = player.server.getLevel(entry.getDimensionKey());
        if (level == null) {
            return false;
        }

        BlockPos pos = entry.getPos();
        if (!canReplaceAt(level, pos)) {
            return false;
        }

        if (entry.getRequiredCount() > 0 && !entry.getRequiredItemId().isEmpty()) {
            if (session.getStoredResourceAmount(entry.getRequiredItemId()) < entry.getRequiredCount()) {
                return false;
            }
        }

        if (!entry.readOriginalState().canSurvive(level, pos)) {
            return false;
        }

        if (!level.setBlock(pos, entry.readOriginalState(), 3)) {
            return false;
        }

        if (entry.getRequiredCount() > 0 && !entry.getRequiredItemId().isEmpty()) {
            session.consumeStoredResource(entry.getRequiredItemId(), entry.getRequiredCount());
        }

        entry.setRestored(true);
        return true;
    }

    private static boolean canReplaceAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced();
    }

    private static int insertIntoStorage(ReconstructionSession session, ItemStack stack) {
        int inserted = 0;
        int remaining = stack.getCount();

        net.minecraft.core.NonNullList<ItemStack> items = session.copyStoredItems();

        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack slotStack = items.get(i);
            if (slotStack.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(slotStack, stack)) {
                continue;
            }

            int max = Math.min(slotStack.getMaxStackSize(), 64);
            int free = max - slotStack.getCount();
            if (free <= 0) {
                continue;
            }

            int toMove = Math.min(free, remaining);
            slotStack.grow(toMove);
            remaining -= toMove;
            inserted += toMove;
        }

        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack slotStack = items.get(i);
            if (!slotStack.isEmpty()) {
                continue;
            }

            int toMove = Math.min(Math.min(stack.getMaxStackSize(), 64), remaining);
            ItemStack newStack = stack.copy();
            newStack.setCount(toMove);
            items.set(i, newStack);

            remaining -= toMove;
            inserted += toMove;
        }

        if (inserted > 0) {
            session.overwriteStoredItems(items);
        }

        return inserted;
    }
}