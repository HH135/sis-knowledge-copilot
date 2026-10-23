package io.github.hyun.sis_app_intelligence.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ProcedureChunker implements ChunkingStrategy {

    private static final int WINDOW_LINES = 300;
    private static final int OVERLAP_LINES = 30;

    @Override
    public List<ChunkDraft> chunk(ChunkingInput input) {
        String description = input.description() == null ? "" : input.description();
        if ("NO_ACCESS".equalsIgnoreCase(input.definitionStatus()) || input.content() == null) {
            String content = input.title() + (description.isBlank() ? "" : "\n" + description);
            return List.of(draft(input, 1, 1, content));
        }

        String[] lines = input.content().split("\\R", -1);
        int step = WINDOW_LINES - OVERLAP_LINES;
        List<ChunkDraft> chunks = new ArrayList<>();
        for (int start = 0; start < lines.length; start += step) {
            int end = Math.min(start + WINDOW_LINES, lines.length);
            String content = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
            chunks.add(draft(input, start + 1, end, content));
            if (end == lines.length) {
                break;
            }
        }
        return List.copyOf(chunks);
    }

    private ChunkDraft draft(ChunkingInput input, int start, int end, String content) {
        return new ChunkDraft(input.origin(), "PROCEDURE", input.sourceRef(), input.title(),
                start, end, content, LineWindowChunker.sha256(content));
    }
}
