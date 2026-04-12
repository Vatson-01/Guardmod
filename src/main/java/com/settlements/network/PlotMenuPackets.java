package com.settlements.network;

import com.settlements.data.model.PlotPermission;
import com.settlements.network.packet.C2SPlotMenuActionPacket;
import com.settlements.world.menu.PlotMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

public final class PlotMenuPackets {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("settlements", "plot_menu"),
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return PROTOCOL_VERSION;
                }
            },
            new java.util.function.Predicate<String>() {
                @Override
                public boolean test(String version) {
                    return PROTOCOL_VERSION.equals(version);
                }
            },
            new java.util.function.Predicate<String>() {
                @Override
                public boolean test(String version) {
                    return PROTOCOL_VERSION.equals(version);
                }
            }
    );

    private static boolean initialized = false;

    private PlotMenuPackets() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        int packetId = 0;
        CHANNEL.registerMessage(
                packetId++,
                C2SPlotMenuActionPacket.class,
                C2SPlotMenuActionPacket::encode,
                C2SPlotMenuActionPacket::decode,
                C2SPlotMenuActionPacket::handle
        );
    }

    public static void sendBack(PlotMenu menu) {
        if (menu == null) {
            return;
        }
        CHANNEL.sendToServer(C2SPlotMenuActionPacket.back(
                menu.getSettlementId(),
                menu.getDimensionId(),
                menu.getChunkX(),
                menu.getChunkZ()
        ));
    }

    public static void sendAssign(PlotMenu menu, UUID targetPlayerUuid) {
        if (menu == null || targetPlayerUuid == null) {
            return;
        }
        CHANNEL.sendToServer(C2SPlotMenuActionPacket.assign(
                menu.getSettlementId(),
                menu.getDimensionId(),
                menu.getChunkX(),
                menu.getChunkZ(),
                targetPlayerUuid
        ));
    }

    public static void sendUnassign(PlotMenu menu) {
        if (menu == null) {
            return;
        }
        CHANNEL.sendToServer(C2SPlotMenuActionPacket.unassign(
                menu.getSettlementId(),
                menu.getDimensionId(),
                menu.getChunkX(),
                menu.getChunkZ()
        ));
    }

    public static void sendTogglePermission(PlotMenu menu, UUID targetPlayerUuid, PlotPermission permission) {
        if (menu == null || targetPlayerUuid == null || permission == null) {
            return;
        }
        CHANNEL.sendToServer(C2SPlotMenuActionPacket.togglePermission(
                menu.getSettlementId(),
                menu.getDimensionId(),
                menu.getChunkX(),
                menu.getChunkZ(),
                targetPlayerUuid,
                permission
        ));
    }
}
