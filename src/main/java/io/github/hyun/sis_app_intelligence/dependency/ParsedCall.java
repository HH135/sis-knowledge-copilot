package io.github.hyun.sis_app_intelligence.dependency;

public record ParsedCall(String schema, String name, String kind, int lineNo) {
}
