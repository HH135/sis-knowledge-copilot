package io.github.hyun.sis_app_intelligence.collector;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;

@Component
public class CharsetResolver {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};

    private final SourceProperties properties;

    public CharsetResolver(SourceProperties properties) {
        this.properties = properties;
    }

    public DecodedContent decode(String relativePath, byte[] bytes) {
        Bom bom = detectBom(bytes);
        String configuredOverride = properties.getCharsetOverrides().get(relativePath);
        Charset charset = configuredOverride == null ? null : Charset.forName(configuredOverride);
        String charsetName = configuredOverride;
        if (charset == null && bom != null) {
            charset = bom.charset();
            charsetName = charset.name();
        }
        if (charset == null) {
            charset = strictUtf8OrNull(bytes);
            if (charset != null) {
                charsetName = charset.name();
            }
        }
        if (charset == null) {
            charsetName = properties.getFallbackCharset();
            charset = Charset.forName(charsetName);
        }

        int offset = bom == null ? 0 : bom.length();
        String content = new String(bytes, offset, bytes.length - offset, charset);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        return new DecodedContent(content, charsetName);
    }

    private Charset strictUtf8OrNull(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        }
        catch (CharacterCodingException exception) {
            return null;
        }
    }

    private Bom detectBom(byte[] bytes) {
        if (startsWith(bytes, UTF8_BOM)) {
            return new Bom(StandardCharsets.UTF_8, UTF8_BOM.length);
        }
        if (startsWith(bytes, UTF16_BE_BOM)) {
            return new Bom(StandardCharsets.UTF_16BE, UTF16_BE_BOM.length);
        }
        if (startsWith(bytes, UTF16_LE_BOM)) {
            return new Bom(StandardCharsets.UTF_16LE, UTF16_LE_BOM.length);
        }
        return null;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    public record DecodedContent(String content, String charsetName) {
    }

    private record Bom(Charset charset, int length) {
    }
}
