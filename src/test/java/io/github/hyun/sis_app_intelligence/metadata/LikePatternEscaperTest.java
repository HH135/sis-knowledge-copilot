package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LikePatternEscaperTest {

    @Test
    void escapesSqlLikeWildcardsAndEscapeCharacter() {
        assertEquals("ORDER\\_NO", LikePatternEscaper.escape("ORDER_NO"));
        assertEquals("\\%\\_\\\\", LikePatternEscaper.escape("%_\\"));
    }
}
