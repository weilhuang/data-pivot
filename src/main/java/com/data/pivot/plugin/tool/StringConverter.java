package com.data.pivot.plugin.tool;

/**
 * Naming conversions used by the default HumpUnderline strategy (and JPA/MP fallbacks).
 * Mirrors the former JS helpers: capture the letter after {@code _} and uppercase it,
 * instead of dropping the underscore and leaving the letter lowercase.
 */
public final class StringConverter {

    private StringConverter() {
    }

    public static String toUnderScore(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    public static String toBigCamelCase(String str) {
        String camel = toCamelBody(str);
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    public static String toCamelCase(String str) {
        String camel = toCamelBody(str);
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toLowerCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * Converts {@code hello_world_test} → {@code helloWorldTest} and {@code USER_ID} → {@code userId}.
     * Consecutive underscores are collapsed. Already-camel identifiers without underscores are kept.
     */
    static String toCamelBody(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String source = looksLikeUpperSnake(str) ? str.toLowerCase() : str;
        StringBuilder out = new StringBuilder(source.length());
        boolean upperNext = false;
        boolean seenNonUnderscore = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '_') {
                if (seenNonUnderscore) {
                    upperNext = true;
                }
                continue;
            }
            seenNonUnderscore = true;
            if (upperNext) {
                out.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static boolean looksLikeUpperSnake(String str) {
        boolean hasLetter = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_' || Character.isDigit(c)) {
                continue;
            }
            if (!Character.isLetter(c)) {
                return false;
            }
            hasLetter = true;
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return hasLetter;
    }
}
