package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

public final class MsmCompatService {
    public static final String MSM_MOD_ID = "siegemachines";
    private static final String MSM_PACKAGE_PREFIX = "ru.magistu.siegemachines.";
    private static final String MSM_EXPLOSION_CLASS = "ru.magistu.siegemachines.entity.projectile.MissileExplosion";

    private MsmCompatService() {
    }

    public static boolean isMsmLoaded() {
        return ModList.get().isLoaded(MSM_MOD_ID);
    }

    public static boolean isMsmExplosion(Explosion explosion) {
        if (explosion == null) {
            return false;
        }

        Class<?> explosionClass = explosion.getClass();
        String className = explosionClass.getName();

        if (MSM_EXPLOSION_CLASS.equals(className)) {
            return true;
        }

        if (className.startsWith(MSM_PACKAGE_PREFIX)) {
            return true;
        }

        if (isMsmEntity(explosion.getDirectSourceEntity())) {
            return true;
        }

        if (isMsmEntity(explosion.getIndirectSourceEntity())) {
            return true;
        }

        return false;
    }

    public static boolean isMsmEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String className = entity.getClass().getName();
        return className.startsWith(MSM_PACKAGE_PREFIX);
    }

    public static UUID resolveAttackerSettlementId(Level level, Explosion explosion) {
        if (level == null || level.getServer() == null || explosion == null) {
            return null;
        }

        ServerPlayer responsiblePlayer = resolveResponsiblePlayer(explosion);
        if (responsiblePlayer == null) {
            return null;
        }

        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        Settlement settlement = data.getSettlementByPlayer(responsiblePlayer.getUUID());
        return settlement == null ? null : settlement.getId();
    }

    public static UUID resolveAttackerSettlementId(Level level, Entity sourceEntity) {
        if (level == null || level.getServer() == null || sourceEntity == null) {
            return null;
        }

        ServerPlayer responsiblePlayer = findPlayerFromEntity(sourceEntity);
        if (responsiblePlayer == null) {
            return null;
        }

        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        Settlement settlement = data.getSettlementByPlayer(responsiblePlayer.getUUID());
        return settlement == null ? null : settlement.getId();
    }

    private static ServerPlayer resolveResponsiblePlayer(Explosion explosion) {
        ServerPlayer player = findPlayerFromEntity(explosion.getIndirectSourceEntity());
        if (player != null) {
            return player;
        }

        player = findPlayerFromEntity(explosion.getDirectSourceEntity());
        if (player != null) {
            return player;
        }

        return null;
    }

    private static ServerPlayer findPlayerFromEntity(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof ServerPlayer) {
            return (ServerPlayer) entity;
        }

        if (entity.getControllingPassenger() instanceof ServerPlayer) {
            return (ServerPlayer) entity.getControllingPassenger();
        }

        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer) {
                return (ServerPlayer) owner;
            }
            if (owner != null && owner.getControllingPassenger() instanceof ServerPlayer) {
                return (ServerPlayer) owner.getControllingPassenger();
            }
        }

        if (entity instanceof PrimedTnt) {
            PrimedTnt primedTnt = (PrimedTnt) entity;
            LivingEntity owner = primedTnt.getOwner();
            if (owner instanceof ServerPlayer) {
                return (ServerPlayer) owner;
            }
        }

        if (entity.getVehicle() instanceof ServerPlayer) {
            return (ServerPlayer) entity.getVehicle();
        }

        if (!entity.getPassengers().isEmpty()) {
            for (Entity passenger : entity.getPassengers()) {
                if (passenger instanceof ServerPlayer) {
                    return (ServerPlayer) passenger;
                }
            }
        }

        return null;
    }
}