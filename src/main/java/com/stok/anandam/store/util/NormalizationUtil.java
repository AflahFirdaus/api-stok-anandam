package com.stok.anandam.store.util;

public class NormalizationUtil {

    public static String normalizeItemName(String name) {
        if (name == null)
            return "";
        return name
                .toUpperCase()
                .replace("™", "")
                .replace("®", "")
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeCompact(String value) {
        if (value == null)
            return "";
        return value.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
