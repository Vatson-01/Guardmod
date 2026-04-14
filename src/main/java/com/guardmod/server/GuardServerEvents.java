package com.guardmod.server;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.net.GuardNetwork;
import com.guardmod.scanner.ModScanService;
import com.guardmod.scanner.ResourcePackScanService;
import com.guardmod.validate.ValidationStateRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.guardmod.validate.ClientEnvironmentReportCache;
import com.guardmod.scanner.ShaderPackScanService;
import com.guardmod.command.GuardModCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class GuardServerEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Map<java.util.UUID, Long> NEXT_PERIODIC_REVALIDATION_AT =
            new java.util.HashMap<java.util.UUID, Long>();

    private static final long PERIODIC_REVALIDATION_BASE_INTERVAL_MILLIS = 30000L;
    private static final long PERIODIC_REVALIDATION_JITTER_MILLIS = 15000L;
    private static final java.util.Random PERIODIC_REVALIDATION_RANDOM = new java.util.Random();

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (GuardCommonConfig.RESCAN_ON_SERVER_START.get()) {
            ModScanService.rescanFromServer(event.getServer());
            ResourcePackScanService.rescanFromServer(event.getServer());
            ShaderPackScanService.rescanFromServer(event.getServer());
        }
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GuardModCommands.register(event.getDispatcher());
    }
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        long timeoutMillis = GuardCommonConfig.VALIDATION_TIMEOUT_SECONDS.get().longValue() * 1000L;
        long deadlineEpochMillis = System.currentTimeMillis() + timeoutMillis;

        ValidationStateRegistry.ValidationTicket ticket = ValidationStateRegistry.createPending(player, deadlineEpochMillis);
        LOGGER.info("GuardMod sent validation request {} to {} with timeout {} ms.",
                ticket.getRequestId(), player.getGameProfile().getName(), timeoutMillis);
        GuardNetwork.sendEnvironmentRequest(player, ticket.getRequestId(), ticket.getDeadlineEpochMillis());
        NEXT_PERIODIC_REVALIDATION_AT.put(player.getUUID(), System.currentTimeMillis() + nextPeriodicDelayMillis());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.player;
        if (player.level().isClientSide()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (ValidationStateRegistry.isPendingTimeout(player, now)) {
            ValidationStateRegistry.clear(player);
            LOGGER.warn("GuardMod validation timed out for {}.", player.getGameProfile().getName());
            player.connection.disconnect(Component.literal("Client validation failed: timeout"));
            return;
        }

        Long nextPeriodicAt = NEXT_PERIODIC_REVALIDATION_AT.get(player.getUUID());
        if (nextPeriodicAt != null && now >= nextPeriodicAt) {
            long timeoutMillis = GuardCommonConfig.VALIDATION_TIMEOUT_SECONDS.get().longValue() * 1000L;
            long deadlineEpochMillis = now + timeoutMillis;

            ValidationStateRegistry.ValidationTicket ticket = ValidationStateRegistry.createPending(player, deadlineEpochMillis);
            NEXT_PERIODIC_REVALIDATION_AT.put(player.getUUID(), now + nextPeriodicDelayMillis());

            LOGGER.info("GuardMod sent periodic validation request {} to {}.",
                    ticket.getRequestId(), player.getGameProfile().getName());

            GuardNetwork.sendEnvironmentRequest(player, ticket.getRequestId(), ticket.getDeadlineEpochMillis());
        }
    }
    private static long nextPeriodicDelayMillis() {
        long jitter = PERIODIC_REVALIDATION_JITTER_MILLIS <= 0L
                ? 0L
                : PERIODIC_REVALIDATION_RANDOM.nextInt((int) PERIODIC_REVALIDATION_JITTER_MILLIS + 1);

        return PERIODIC_REVALIDATION_BASE_INTERVAL_MILLIS + jitter;
    }
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ValidationStateRegistry.clear(player);
            ClientEnvironmentReportCache.remove(player);
            NEXT_PERIODIC_REVALIDATION_AT.remove(player.getUUID());
        }
    }
}