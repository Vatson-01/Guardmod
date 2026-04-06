package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class PlotPermissionSet {
    private final EnumSet<PlotPermission> permissions = EnumSet.noneOf(PlotPermission.class);

    public boolean has(PlotPermission permission) {
        return permissions.contains(permission);
    }

    public void grant(PlotPermission permission) {
        permissions.add(permission);
    }

    public void revoke(PlotPermission permission) {
        permissions.remove(permission);
    }

    public Set<PlotPermission> asReadOnlySet() {
        return Collections.unmodifiableSet(permissions);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (PlotPermission permission : permissions) {
            listTag.add(StringTag.valueOf(permission.name()));
        }

        tag.put("Permissions", listTag);
        return tag;
    }

    public static PlotPermissionSet load(CompoundTag tag) {
        PlotPermissionSet set = new PlotPermissionSet();

        if (tag == null || !tag.contains("Permissions", Tag.TAG_LIST)) {
            return set;
        }

        ListTag listTag = tag.getList("Permissions", Tag.TAG_STRING);
        for (int i = 0; i < listTag.size(); i++) {
            String raw = listTag.getString(i);
            try {
                set.grant(PlotPermission.valueOf(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return set;
    }
}