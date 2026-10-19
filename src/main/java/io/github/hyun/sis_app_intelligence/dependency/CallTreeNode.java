package io.github.hyun.sis_app_intelligence.dependency;

import java.util.List;

public record CallTreeNode(
        String procedure,
        String callType,
        Integer lineNo,
        boolean cycle,
        List<CallTreeNode> children
) {
}
