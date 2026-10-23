package io.github.hyun.sis_app_intelligence.document;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TableChunker implements ChunkingStrategy {

    @Override
    public List<ChunkDraft> chunk(ChunkingInput input) {
        String content = input.title() + " / " + (input.description() == null ? "" : input.description())
                + "\n" + input.content();
        return List.of(new ChunkDraft(
                input.origin(),
                "TABLE",
                input.sourceRef(),
                input.title(),
                1,
                1,
                content,
                LineWindowChunker.sha256(content)));
    }
}
