package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class SettlementPermissionSet {
    private final EnumSet<SettlementPermission> permissions;

    public SettlementPermissionSet() {
        this.permissions = EnumSet.noneOf(SettlementPermission.class);
    }

    public boolean has(SettlementPermission permission) {
        return permission != null && permissions.contains(permission);
    }

    public void grant(SettlementPermission permission) {
        if (permission == null) {
            return;
        }
        permissions.add(permission);
    }

    public void revoke(SettlementPermission permission) {
        if (permission == null) {
            return;
        }
        permissions.remove(permission);
    }

    public Set<SettlementPermission> asReadOnlySet() {
        if (permissions.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        ListTag permissionsTag = new ListTag();
        for (SettlementPermission permission : permissions) {
            permissionsTag.add(StringTag.valueOf(permission.name()));
        }

        tag.put("Permissions", permissionsTag);
        return tag;
    }

    public static SettlementPermissionSet load(CompoundTag tag) {
        SettlementPermissionSet set = new SettlementPermissionSet();
        if (tag == null) {
            return set;
        }

        if (tag.contains("Permissions", Tag.TAG_LIST)) {
            ListTag permissionsTag = tag.getList("Permissions", Tag.TAG_STRING);
            for (int i = 0; i < permissionsTag.size(); i++) {
                String name = permissionsTag.getString(i);
                try {
                    set.grant(SettlementPermission.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return set;
    }
}