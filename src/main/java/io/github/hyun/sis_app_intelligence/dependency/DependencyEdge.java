package io.github.hyun.sis_app_intelligence.dependency;

public record DependencyEdge(
        String origin,
        String callerType,
        String callerRef,
        String calleeSchema,
        String calleeName,
        String callType,
        Integer lineNo
) {
}
