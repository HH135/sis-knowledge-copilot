package io.github.hyun.sis_app_intelligence.collector;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import io.github.hyun.sis_app_intelligence.source.SourceFile;

final class SourceFileSupport {

    private SourceFileSupport() {
    }

    static SourceFile create(
            String origin,
            String relativePath,
            String extension,
            byte[] bytes,
            CharsetResolver charsetResolver) {
        CharsetResolver.DecodedContent decoded = charsetResolver.decode(relativePath, bytes);
        return new SourceFile(origin, relativePath, extension, decoded.charsetName(), decoded.content(),
                sha256(bytes), bytes.length);
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
