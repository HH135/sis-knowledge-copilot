package io.github.hyun.sis_app_intelligence.document;

import java.util.List;

public interface ChunkingStrategy {

    List<ChunkDraft> chunk(ChunkingInput input);
}
