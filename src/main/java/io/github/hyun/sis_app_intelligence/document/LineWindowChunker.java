package io.github.hyun.sis_app_intelligence.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LineWindowChunker implements ChunkingStrategy {

    private static final int WINDOW_LINES = 120;
    private static final int OVERLAP_LINES = 20;

    @Override
    public List<ChunkDraft> chunk(ChunkingInput input) {
        String[] lines = input.content().split("\\R", -1);
        int step = WINDOW_LINES - OVERLAP_LINES;
        List<ChunkDraft> chunks = new ArrayList<>();
        for (int start = 0; start < lines.length; start += step) {
            int end = Math.min(start + WINDOW_LINES, lines.length);
            String content = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
            chunks.add(new ChunkDraft(
                    input.origin(),
                    "FILE",
                    input.sourceRef(),
                    input.title(),
                    start + 1,
                    end,
                    content,
                    sha256(content)));
            if (end == lines.length) {
                break;
            }
        }
        return List.copyOf(chunks);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
