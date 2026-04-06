package com.settlements.data;

import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SettlementPlot;
import com.settlements.data.model.ShopRecord;
import com.settlements.util.BlockPosKeyUtil;
import com.settlements.util.ClaimKeyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SettlementSavedData extends SavedData {
    public static final String DATA_NAME = "settlements_data";

    private final Map<UUID, Settlement> settlementsById = new LinkedHashMap<UUID, Settlement>();
    private final Map<String, UUID> settlementIdByNameLower = new LinkedHashMap<String, UUID>();
    private final Map<UUID, UUID> settlementIdByPlayer = new LinkedHashMap<UUID, UUID>();
    private final Map<String, SettlementChunkClaim> claimsByKey = new LinkedHashMap<String, SettlementChunkClaim>();

    private final Map<UUID, SettlementPlot> plotsById = new LinkedHashMap<UUID, SettlementPlot>();
    private final Map<String, UUID> plotIdByChunkKey = new LinkedHashMap<String, UUID>();
    private final Map<String, UUID> plotIdByOwnerKey = new LinkedHashMap<String, UUID>();

    private final Map<UUID, ShopRecord> shopsById = new LinkedHashMap<UUID, ShopRecord>();
    private final Map<String, UUID> shopIdByPosKey = new LinkedHashMap<String, UUID>();

    public SettlementSavedData() {
    }

    public static SettlementSavedData load(CompoundTag tag) {
        SettlementSavedData data = new SettlementSavedData();

        if (tag.contains("Settlements", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Settlements", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag settlementTag = listTag.getCompound(i);
                Settlement settlement = Settlement.load(settlementTag);
                data.settlementsById.put(settlement.getId(), settlement);
            }
        }

        if (tag.contains("Claims", Tag.TAG_LIST)) {
            ListTag claimList = tag.getList("Claims", Tag.TAG_COMPOUND);
            for (int i = 0; i < claimList.size(); i++) {
                SettlementChunkClaim claim = SettlementChunkClaim.load(claimList.getCompound(i));
                if (data.settlementsById.containsKey(claim.getSettlementId())) {
                    data.claimsByKey.put(claim.getChunkKey(), claim);
                    data.settlementsById.get(claim.getSettlementId()).addClaimedChunkKey(claim.getChunkKey(), 0L);
                }
            }
        }

        if (tag.contains("Plots", Tag.TAG_LIST)) {
            ListTag plotList = tag.getList("Plots", Tag.TAG_COMPOUND);
            for (int i = 0; i < plotList.size(); i++) {
                SettlementPlot plot = SettlementPlot.load(plotList.getCompound(i));
                if (data.settlementsById.containsKey(plot.getSettlementId())) {
                    data.plotsById.put(plot.getId(), plot);
                }
            }
        }

        if (tag.contains("Shops", Tag.TAG_LIST)) {
            ListTag shopList = tag.getList("Shops", Tag.TAG_COMPOUND);
            for (int i = 0; i < shopList.size(); i++) {
                ShopRecord shop = ShopRecord.load(shopList.getCompound(i));

                if (shop.isAdminShop()) {
                    data.shopsById.put(shop.getId(), shop);
                } else if (shop.getSettlementId() != null && data.settlementsById.containsKey(shop.getSettlementId())) {
                    data.shopsById.put(shop.getId(), shop);
                }
            }
        }

        data.rebuildIndexes();
        return data;
    }

    public static SettlementSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(SettlementSavedData::load, SettlementSavedData::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag settlementsTag = new ListTag();
        for (Settlement settlement : settlementsById.values()) {
            settlementsTag.add(settlement.save());
        }
        tag.put("Settlements", settlementsTag);

        ListTag claimsTag = new ListTag();
        for (SettlementChunkClaim claim : claimsByKey.values()) {
            claimsTag.add(claim.save());
        }
        tag.put("Claims", claimsTag);

        ListTag plotsTag = new ListTag();
        for (SettlementPlot plot : plotsById.values()) {
            plotsTag.add(plot.save());
        }
        tag.put("Plots", plotsTag);

        ListTag shopsTag = new ListTag();
        for (ShopRecord shop : shopsById.values()) {
            shopsTag.add(shop.save());
        }
        tag.put("Shops", shopsTag);

        return tag;
    }

    public Collection<Settlement> getAllSettlements() {
        return Collections.unmodifiableCollection(settlementsById.values());
    }

    public Settlement getSettlement(UUID settlementId) {
        return settlementsById.get(settlementId);
    }

    public Settlement getSettlementByPlayer(UUID playerUuid) {
        UUID settlementId = settlementIdByPlayer.get(playerUuid);
        return settlementId == null ? null : settlementsById.get(settlementId);
    }

    public Settlement getSettlementByName(String name) {
        if (name == null) {
            return null;
        }

        UUID settlementId = settlementIdByNameLower.get(name.trim().toLowerCase());
        return settlementId == null ? null : settlementsById.get(settlementId);
    }

    public SettlementChunkClaim getClaim(Level level, ChunkPos chunkPos) {
        return claimsByKey.get(ClaimKeyUtil.toKey(level.dimension(), chunkPos));
    }

    public SettlementChunkClaim getClaim(net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return claimsByKey.get(ClaimKeyUtil.toKey(dimension, chunkPos));
    }

    public Settlement getSettlementByChunk(Level level, ChunkPos chunkPos) {
        SettlementChunkClaim claim = getClaim(level, chunkPos);
        return claim == null ? null : settlementsById.get(claim.getSettlementId());
    }

    public boolean isChunkClaimed(Level level, ChunkPos chunkPos) {
        return getClaim(level, chunkPos) != null;
    }

    public SettlementPlot getPlotByChunk(Level level, ChunkPos chunkPos) {
        return getPlotByChunkKey(ClaimKeyUtil.toKey(level.dimension(), chunkPos));
    }

    public SettlementPlot getPlotByChunkKey(String chunkKey) {
        UUID plotId = plotIdByChunkKey.get(chunkKey);
        return plotId == null ? null : plotsById.get(plotId);
    }

    public SettlementPlot getPlotByOwner(UUID settlementId, UUID ownerUuid) {
        UUID plotId = plotIdByOwnerKey.get(buildPlotOwnerKey(settlementId, ownerUuid));
        return plotId == null ? null : plotsById.get(plotId);
    }

    public SettlementPlot getOrCreatePlotForOwner(UUID settlementId, UUID ownerUuid, long gameTime) {
        SettlementPlot existing = getPlotByOwner(settlementId, ownerUuid);
        if (existing != null) {
            return existing;
        }

        SettlementPlot created = SettlementPlot.createNew(settlementId, ownerUuid, gameTime);
        plotsById.put(created.getId(), created);
        rebuildIndexes();
        setDirty();
        return created;
    }

    public ShopRecord getShop(UUID shopId) {
        return shopsById.get(shopId);
    }

    public ShopRecord getShopByPos(Level level, BlockPos pos) {
        return getShopByPos(level.dimension(), pos);
    }

    public ShopRecord getShopByPos(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
        UUID shopId = shopIdByPosKey.get(BlockPosKeyUtil.toKey(dimension, pos));
        return shopId == null ? null : shopsById.get(shopId);
    }

    public List<ShopRecord> getShopsByOwner(UUID settlementId, UUID ownerUuid) {
        List<ShopRecord> result = new ArrayList<ShopRecord>();

        for (ShopRecord record : shopsById.values()) {
            if (!record.isPlayerShop()) {
                continue;
            }

            if (record.getSettlementId() != null
                    && record.getSettlementId().equals(settlementId)
                    && record.getOwnerUuid() != null
                    && record.getOwnerUuid().equals(ownerUuid)) {
                result.add(record);
            }
        }

        return result;
    }

    public void addSettlement(Settlement settlement) {
        settlementsById.put(settlement.getId(), settlement);
        rebuildIndexes();
        setDirty();
    }

    public void removeSettlement(UUID settlementId) {
        Settlement removed = settlementsById.remove(settlementId);
        if (removed != null) {
            for (String chunkKey : removed.getClaimedChunkKeys()) {
                claimsByKey.remove(chunkKey);
            }
        }

        plotsById.entrySet().removeIf(entry -> entry.getValue().getSettlementId().equals(settlementId));
        shopsById.entrySet().removeIf(entry -> entry.getValue().getSettlementId() != null && entry.getValue().getSettlementId().equals(settlementId));

        rebuildIndexes();
        setDirty();
    }

    public void addClaim(SettlementChunkClaim claim, long gameTime) {
        Settlement settlement = settlementsById.get(claim.getSettlementId());
        if (settlement == null) {
            throw new IllegalStateException("Поселение для клейма не найдено.");
        }

        claimsByKey.put(claim.getChunkKey(), claim);
        settlement.addClaimedChunkKey(claim.getChunkKey(), gameTime);
        rebuildIndexes();
        setDirty();
    }

    public void removeClaim(net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunkPos, long gameTime) {
        String key = ClaimKeyUtil.toKey(dimension, chunkPos);
        SettlementChunkClaim removed = claimsByKey.remove(key);

        if (removed != null) {
            Settlement settlement = settlementsById.get(removed.getSettlementId());
            if (settlement != null) {
                settlement.removeClaimedChunkKey(key, gameTime);
            }
        }

        SettlementPlot plot = getPlotByChunkKey(key);
        if (plot != null) {
            plot.removeChunkKey(key, gameTime);
            if (plot.isEmpty()) {
                plotsById.remove(plot.getId());
            }
        }

        rebuildIndexes();
        setDirty();
    }

    public void saveOrUpdatePlot(SettlementPlot plot) {
        plotsById.put(plot.getId(), plot);
        rebuildIndexes();
        setDirty();
    }

    public void removePlot(UUID plotId) {
        plotsById.remove(plotId);
        rebuildIndexes();
        setDirty();
    }

    public void addShop(ShopRecord shop) {
        shopsById.put(shop.getId(), shop);
        rebuildIndexes();
        setDirty();
    }

    public void updateShop(ShopRecord shop) {
        shopsById.put(shop.getId(), shop);
        rebuildIndexes();
        setDirty();
    }

    public void removeShop(UUID shopId) {
        shopsById.remove(shopId);
        rebuildIndexes();
        setDirty();
    }

    public void markChanged() {
        rebuildIndexes();
        setDirty();
    }

    private void rebuildIndexes() {
        settlementIdByNameLower.clear();
        settlementIdByPlayer.clear();
        plotIdByChunkKey.clear();
        plotIdByOwnerKey.clear();
        shopIdByPosKey.clear();

        for (Settlement settlement : settlementsById.values()) {
            settlementIdByNameLower.put(settlement.getName().trim().toLowerCase(), settlement.getId());
            settlement.getMemberMap().forEach((playerUuid, member) -> settlementIdByPlayer.put(playerUuid, settlement.getId()));
        }

        for (SettlementPlot plot : plotsById.values()) {
            plotIdByOwnerKey.put(buildPlotOwnerKey(plot.getSettlementId(), plot.getOwnerUuid()), plot.getId());

            for (String chunkKey : plot.getChunkKeys()) {
                plotIdByChunkKey.put(chunkKey, plot.getId());
            }
        }

        for (ShopRecord shop : shopsById.values()) {
            shopIdByPosKey.put(shop.getPosKey(), shop.getId());
        }
    }

    private String buildPlotOwnerKey(UUID settlementId, UUID ownerUuid) {
        return settlementId + "|" + ownerUuid;
    }
}