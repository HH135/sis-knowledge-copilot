package io.github.hyun.sis_app_intelligence.metadata;

public final class LikePatternEscaper {

    private LikePatternEscaper() {
    }

    public static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
