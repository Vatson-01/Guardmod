package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class SettlementService {
    private SettlementService() {
    }

    public static Settlement createSettlement(MinecraftServer server, UUID leaderUuid, String rawName, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        String name = normalizeName(rawName);

        if (name.length() < 3) {
            throw new IllegalArgumentException("Название поселения должно быть не короче 3 символов.");
        }
        if (name.length() > 32) {
            throw new IllegalArgumentException("Название поселения должно быть не длиннее 32 символов.");
        }
        if (data.getSettlementByPlayer(leaderUuid) != null) {
            throw new IllegalStateException("Игрок уже состоит в поселении.");
        }
        if (data.getSettlementByName(name) != null) {
            throw new IllegalStateException("Поселение с таким названием уже существует.");
        }

        Settlement settlement = Settlement.createNew(name, leaderUuid, gameTime);
        data.addSettlement(settlement);
        return settlement;
    }

    public static void disbandSettlement(MinecraftServer server, UUID settlementId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        data.removeSettlement(settlementId);
    }

    public static void disbandSettlementOfPlayer(MinecraftServer server, UUID playerUuid) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlementByPlayer(playerUuid);
        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }
        if (!settlement.isLeader(playerUuid)) {
            throw new IllegalStateException("Распустить поселение может только глава.");
        }
        data.removeSettlement(settlement.getId());
    }

    public static void transferLeader(MinecraftServer server, UUID settlementId, UUID newLeaderUuid, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);

        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        if (!settlement.isResident(newLeaderUuid)) {
            throw new IllegalStateException("Новый глава должен быть жителем поселения.");
        }
        if (settlement.getLeaderUuid().equals(newLeaderUuid)) {
            return;
        }

        settlement.transferLeader(newLeaderUuid, gameTime);
        data.markChanged();
    }

    /**
     * Новый безопасный вариант: добавление жителя с проверкой прав актёра.
     */
    public static void addMember(MinecraftServer server, UUID settlementId, UUID actorUuid, UUID targetUuid, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);

        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        if (data.getSettlementByPlayer(targetUuid) != null) {
            throw new IllegalStateException("Игрок уже состоит в поселении.");
        }

        SettlementMember actor = settlement.getMember(actorUuid);
        if (actor == null) {
            throw new IllegalStateException("Актёр не состоит в этом поселении.");
        }

        if (!canInvite(settlement, actorUuid, actor)) {
            throw new IllegalStateException("У игрока нет права добавлять жителей.");
        }

        settlement.addMember(targetUuid, gameTime);
        data.markChanged();
    }

    /**
     * Новый безопасный вариант: удаление жителя с проверкой прав актёра.
     */
    public static void removeMember(MinecraftServer server, UUID settlementId, UUID actorUuid, UUID targetUuid, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);

        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        if (!settlement.isResident(targetUuid)) {
            throw new IllegalStateException("Игрок не состоит в этом поселении.");
        }
        if (settlement.isLeader(targetUuid)) {
            throw new IllegalStateException("Нельзя удалить главу поселения.");
        }

        SettlementMember actor = settlement.getMember(actorUuid);
        if (actor == null) {
            throw new IllegalStateException("Актёр не состоит в этом поселении.");
        }

        if (!canKick(settlement, actorUuid, actor)) {
            throw new IllegalStateException("У игрока нет права удалять жителей.");
        }

        PlotService.transferPlotsToLeaderOnMemberLeave(data, settlement.getId(), targetUuid, settlement.getLeaderUuid(), gameTime);
        ShopService.transferShopsToLeaderOnMemberLeave(data, settlement.getId(), targetUuid, settlement.getLeaderUuid(), gameTime);
        settlement.removeMember(targetUuid, gameTime);
        data.markChanged();
    }

    /**
     * Старый вариант оставлен только для совместимости.
     * Для нормальной работы прав переведи вызовы на overload с actorUuid.
     */
    @Deprecated
    public static void addMember(MinecraftServer server, UUID settlementId, UUID playerUuid, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        if (data.getSettlementByPlayer(playerUuid) != null) {
            throw new IllegalStateException("Игрок уже состоит в поселении.");
        }
        settlement.addMember(playerUuid, gameTime);
        data.markChanged();
    }

    /**
     * Старый вариант оставлен только для совместимости.
     * Для нормальной работы прав переведи вызовы на overload с actorUuid.
     */
    @Deprecated
    public static void removeMember(MinecraftServer server, UUID settlementId, UUID playerUuid, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }
        if (!settlement.isResident(playerUuid)) {
            throw new IllegalStateException("Игрок не состоит в этом поселении.");
        }
        if (settlement.isLeader(playerUuid)) {
            throw new IllegalStateException("Нельзя удалить главу поселения.");
        }

        PlotService.transferPlotsToLeaderOnMemberLeave(data, settlement.getId(), playerUuid, settlement.getLeaderUuid(), gameTime);
        ShopService.transferShopsToLeaderOnMemberLeave(data, settlement.getId(), playerUuid, settlement.getLeaderUuid(), gameTime);
        settlement.removeMember(playerUuid, gameTime);
        data.markChanged();
    }

    public static void renameSettlement(MinecraftServer server, UUID settlementId, String rawName, long gameTime) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }

        String newName = normalizeName(rawName);
        Settlement existing = data.getSettlementByName(newName);
        if (existing != null && !existing.getId().equals(settlementId)) {
            throw new IllegalStateException("Поселение с таким названием уже существует.");
        }

        settlement.setName(newName, gameTime);
        data.markChanged();
    }

    private static boolean canInvite(Settlement settlement, UUID actorUuid, SettlementMember actor) {
        if (settlement.isLeader(actorUuid)) {
            return true;
        }
        return actor.getPermissionSet().has(SettlementPermission.INVITE_PLAYERS);
    }

    private static boolean canKick(Settlement settlement, UUID actorUuid, SettlementMember actor) {
        if (settlement.isLeader(actorUuid)) {
            return true;
        }
        return actor.getPermissionSet().has(SettlementPermission.KICK_PLAYERS);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.trim().replaceAll("\\s{2,}", " ");
    }
}