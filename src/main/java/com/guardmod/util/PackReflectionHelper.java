package com.guardmod.util;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class PackReflectionHelper {
    private static final int MAX_DEPTH = 5;

    private PackReflectionHelper() {
    }

    public static Path findFirstPath(Object root) {
        return findFirstPath(root, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()), 0);
    }

    private static Path findFirstPath(Object value, Set<Object> visited, int depth) {
        if (value == null || depth > MAX_DEPTH) {
            return null;
        }

        if (value instanceof Path) {
            return ((Path) value).toAbsolutePath().normalize();
        }

        if (value instanceof File) {
            return ((File) value).toPath().toAbsolutePath().normalize();
        }

        if (isLeafValue(value)) {
            return null;
        }

        if (!visited.add(value)) {
            return null;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Path found = findFirstPath(Array.get(value, i), visited, depth + 1);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        if (value instanceof Iterable) {
            for (Object nested : (Iterable<?>) value) {
                Path found = findFirstPath(nested, visited, depth + 1);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Path found = findFirstPath(field.get(value), visited, depth + 1);
                    if (found != null) {
                        return found;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        return null;
    }

    private static boolean isLeafValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum;
    }
}
