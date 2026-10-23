package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationValidatorTest {

    @Test
    void removesOutOfRangeReferencesAndReturnsOnlyCitedContexts() {
        List<RetrievedChunk> contexts = List.of(
                chunk(1, "first"),
                chunk(2, "second"));

        CitationValidator.ValidationResult result = new CitationValidator()
                .validate("Evidence [1]. Invalid [3] and [0]. Again [1].", contexts);

        assertEquals("Evidence [1]. Invalid  and . Again [1].", result.answer());
        assertEquals(2, result.invalidCitations());
        assertEquals(List.of(1), result.citations().stream()
                .map(CitationValidator.CitedContext::no).toList());
    }

    private RetrievedChunk chunk(long id, String title) {
        return new RetrievedChunk(id, "FILE", title, title, 1, 2, "content",
                null, null, 0, false, null);
    }
}
