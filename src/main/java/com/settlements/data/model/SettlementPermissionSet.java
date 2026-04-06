package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class SettlementPermissionSet {
    private final EnumSet<SettlementPermission> permissions = EnumSet.noneOf(SettlementPermission.class);

    public boolean has(SettlementPermission permission) {
        return permissions.contains(permission);
    }

    public void grant(SettlementPermission permission) {
        permissions.add(permission);
    }

    public void revoke(SettlementPermission permission) {
        permissions.remove(permission);
    }

    public void clear() {
        permissions.clear();
    }

    public Set<SettlementPermission> asReadOnlySet() {
        return Collections.unmodifiableSet(permissions);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (SettlementPermission permission : permissions) {
            listTag.add(StringTag.valueOf(permission.name()));
        }

        tag.put("Permissions", listTag);
        return tag;
    }

    public static SettlementPermissionSet load(CompoundTag tag) {
        SettlementPermissionSet set = new SettlementPermissionSet();

        if (tag == null || !tag.contains("Permissions", Tag.TAG_LIST)) {
            return set;
        }

        ListTag listTag = tag.getList("Permissions", Tag.TAG_STRING);
        for (int i = 0; i < listTag.size(); i++) {
            String raw = listTag.getString(i);
            try {
                set.grant(SettlementPermission.valueOf(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return set;
    }
}