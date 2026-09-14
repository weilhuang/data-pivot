package com.data.pivot.plugin.mapping;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared name comparison used by Query/Analysis/gutter when Settings mapping is absent.
 * Multiple fuzzy hits are {@link MappingConfidence#AMBIGUOUS} — never "max similarity wins".
 */
public final class NameMatcher {
    public static final double FUZZY_THRESHOLD = 0.9;
    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    private NameMatcher() {
    }

    public static boolean isSimilar(@Nullable String left, @Nullable String right) {
        return score(left, right) >= FUZZY_THRESHOLD;
    }

    public static double score(@Nullable String left, @Nullable String right) {
        if (left == null || right == null) {
            return 0;
        }
        return JARO_WINKLER.apply(preprocess(left), preprocess(right));
    }

    public static @NotNull String preprocess(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replace("_", "");
    }

    public static <T> @NotNull NameMatch<T> pickUnique(@NotNull List<Scored<T>> scored) {
        List<Scored<T>> exact = new ArrayList<>();
        List<Scored<T>> fuzzy = new ArrayList<>();
        for (Scored<T> item : scored) {
            if (item.score() >= 1.0d) {
                exact.add(item);
            } else if (item.score() >= FUZZY_THRESHOLD) {
                fuzzy.add(item);
            }
        }
        if (exact.size() == 1) {
            return NameMatch.hit(MappingConfidence.EXACT, exact.get(0).value());
        }
        if (exact.size() > 1) {
            return NameMatch.ambiguous(mapValues(exact));
        }
        if (fuzzy.size() == 1) {
            return NameMatch.hit(MappingConfidence.FUZZY, fuzzy.get(0).value());
        }
        if (fuzzy.size() > 1) {
            return NameMatch.ambiguous(mapValues(fuzzy));
        }
        return NameMatch.unresolved();
    }

    private static <T> List<T> mapValues(List<Scored<T>> scored) {
        List<T> values = new ArrayList<>(scored.size());
        for (Scored<T> item : scored) {
            values.add(item.value());
        }
        return values;
    }

    public record Scored<T>(T value, double score) {
    }

    public record NameMatch<T>(MappingConfidence confidence, @Nullable T value, @NotNull List<T> candidates) {
        public static <T> NameMatch<T> hit(MappingConfidence confidence, T value) {
            return new NameMatch<>(confidence, value, List.of(value));
        }

        public static <T> NameMatch<T> ambiguous(List<T> candidates) {
            return new NameMatch<>(MappingConfidence.AMBIGUOUS, null, List.copyOf(candidates));
        }

        public static <T> NameMatch<T> unresolved() {
            return new NameMatch<>(MappingConfidence.UNRESOLVED, null, List.of());
        }

        public boolean isAmbiguous() {
            return confidence == MappingConfidence.AMBIGUOUS;
        }

        public boolean isResolved() {
            return value != null
                    && (confidence == MappingConfidence.EXACT
                    || confidence == MappingConfidence.NAMING
                    || confidence == MappingConfidence.FUZZY);
        }
    }
}
