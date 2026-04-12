package com.settlements.network.packet;

import com.settlements.data.model.PlotPermission;
import com.settlements.service.PlotMenuService;
import com.settlements.service.PlotService;
import com.settlements.service.SettlementMenuService;
import com.settlements.world.menu.PlotMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SPlotMenuActionPacket {
    public enum Action {
        BACK,
        ASSIGN,
        UNASSIGN,
        TOGGLE_PERMISSION
    }

    private final Action action;
    private final UUID settlementId;
    private final String dimensionId;
    private final int chunkX;
    private final int chunkZ;
    private final UUID targetPlayerUuid;
    private final String permissionName;

    public C2SPlotMenuActionPacket(
            Action action,
            UUID settlementId,
            String dimensionId,
            int chunkX,
            int chunkZ,
            UUID targetPlayerUuid,
            String permissionName
    ) {
        this.action = action;
        this.settlementId = settlementId;
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.targetPlayerUuid = targetPlayerUuid;
        this.permissionName = permissionName;
    }

    public static C2SPlotMenuActionPacket back(UUID settlementId, String dimensionId, int chunkX, int chunkZ) {
        return new C2SPlotMenuActionPacket(Action.BACK, settlementId, dimensionId, chunkX, chunkZ, null, null);
    }

    public static C2SPlotMenuActionPacket assign(UUID settlementId, String dimensionId, int chunkX, int chunkZ, UUID targetPlayerUuid) {
        return new C2SPlotMenuActionPacket(Action.ASSIGN, settlementId, dimensionId, chunkX, chunkZ, targetPlayerUuid, null);
    }

    public static C2SPlotMenuActionPacket unassign(UUID settlementId, String dimensionId, int chunkX, int chunkZ) {
        return new C2SPlotMenuActionPacket(Action.UNASSIGN, settlementId, dimensionId, chunkX, chunkZ, null, null);
    }

    public static C2SPlotMenuActionPacket togglePermission(
            UUID settlementId,
            String dimensionId,
            int chunkX,
            int chunkZ,
            UUID targetPlayerUuid,
            PlotPermission permission
    ) {
        return new C2SPlotMenuActionPacket(
                Action.TOGGLE_PERMISSION,
                settlementId,
                dimensionId,
                chunkX,
                chunkZ,
                targetPlayerUuid,
                permission == null ? null : permission.name()
        );
    }

    public static void encode(C2SPlotMenuActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUUID(packet.settlementId);
        buf.writeUtf(packet.dimensionId);
        buf.writeInt(packet.chunkX);
        buf.writeInt(packet.chunkZ);

        buf.writeBoolean(packet.targetPlayerUuid != null);
        if (packet.targetPlayerUuid != null) {
            buf.writeUUID(packet.targetPlayerUuid);
        }

        buf.writeBoolean(packet.permissionName != null);
        if (packet.permissionName != null) {
            buf.writeUtf(packet.permissionName);
        }
    }

    public static C2SPlotMenuActionPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        UUID settlementId = buf.readUUID();
        String dimensionId = buf.readUtf();
        int chunkX = buf.readInt();
        int chunkZ = buf.readInt();

        UUID targetPlayerUuid = null;
        if (buf.readBoolean()) {
            targetPlayerUuid = buf.readUUID();
        }

        String permissionName = null;
        if (buf.readBoolean()) {
            permissionName = buf.readUtf();
        }

        return new C2SPlotMenuActionPacket(action, settlementId, dimensionId, chunkX, chunkZ, targetPlayerUuid, permissionName);
    }

    public static void handle(final C2SPlotMenuActionPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(new Runnable() {
            @Override
            public void run() {
                ServerPlayer sender = context.getSender();
                if (sender == null || packet == null || packet.settlementId == null || packet.dimensionId == null) {
                    return;
                }

                AbstractContainerMenu currentMenu = sender.containerMenu;
                if (!(currentMenu instanceof PlotMenu)) {
                    sender.displayClientMessage(Component.literal("Меню участка не открыто."), false);
                    return;
                }

                PlotMenu plotMenu = (PlotMenu) currentMenu;
                if (!packetMatchesMenu(packet, plotMenu)) {
                    sender.displayClientMessage(Component.literal("Данные меню участка устарели."), false);
                    return;
                }

                ResourceKey<Level> dimensionKey = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new ResourceLocation(packet.dimensionId)
                );
                ChunkPos chunkPos = new ChunkPos(packet.chunkX, packet.chunkZ);

                try {
                    if (packet.action == Action.BACK) {
                        SettlementMenuService.openMenu(sender, packet.settlementId);
                        return;
                    }

                    if (packet.action == Action.ASSIGN) {
                        if (packet.targetPlayerUuid == null) {
                            throw new IllegalStateException("Не выбран житель для назначения участка.");
                        }

                        PlotService.assignChunkToPlayer(sender, packet.targetPlayerUuid, dimensionKey, chunkPos);
                        PlotMenuService.openMenu(sender, packet.settlementId, dimensionKey, chunkPos, plotMenu.getPage(), plotMenu.getSelectedIndex());
                        sender.displayClientMessage(Component.literal("Чанк назначен выбранному жителю."), false);
                        return;
                    }

                    if (packet.action == Action.UNASSIGN) {
                        PlotService.unassignChunk(sender, dimensionKey, chunkPos);
                        PlotMenuService.openMenu(sender, packet.settlementId, dimensionKey, chunkPos, plotMenu.getPage(), plotMenu.getSelectedIndex());
                        sender.displayClientMessage(Component.literal("Чанк снова стал общей территорией."), false);
                        return;
                    }

                    if (packet.action == Action.TOGGLE_PERMISSION) {
                        if (packet.targetPlayerUuid == null || packet.permissionName == null || packet.permissionName.trim().isEmpty()) {
                            throw new IllegalStateException("Не хватает данных для изменения локального доступа.");
                        }

                        PlotPermission permission = PlotPermission.valueOf(packet.permissionName);
                        com.settlements.world.menu.PlotPlayerView selectedView = plotMenu.getSelectedPlayerView();
                        if (selectedView == null) {
                            throw new IllegalStateException("Игрок не выбран.");
                        }
                        if (selectedView.isOwner()) {
                            throw new IllegalStateException("Нельзя менять локальные права владельца участка.");
                        }

                        if (selectedView.hasPermission(permission)) {
                            PlotService.revokePermissionOnPlot(sender, packet.targetPlayerUuid, permission, dimensionKey, chunkPos);
                            sender.displayClientMessage(Component.literal("Локальный доступ снят."), false);
                        } else {
                            PlotService.grantPermissionOnPlot(sender, packet.targetPlayerUuid, permission, dimensionKey, chunkPos);
                            sender.displayClientMessage(Component.literal("Локальный доступ выдан."), false);
                        }

                        PlotMenuService.openMenu(sender, packet.settlementId, dimensionKey, chunkPos, plotMenu.getPage(), plotMenu.getSelectedIndex());
                    }
                } catch (Exception ex) {
                    String message = ex.getMessage();
                    if (message == null || message.trim().isEmpty()) {
                        message = "Ошибка управления участком.";
                    }
                    sender.displayClientMessage(Component.literal(message), false);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean packetMatchesMenu(C2SPlotMenuActionPacket packet, PlotMenu menu) {
        if (packet == null || menu == null) {
            return false;
        }
        if (!packet.settlementId.equals(menu.getSettlementId())) {
            return false;
        }
        if (!packet.dimensionId.equals(menu.getDimensionId())) {
            return false;
        }
        return packet.chunkX == menu.getChunkX() && packet.chunkZ == menu.getChunkZ();
    }
}
