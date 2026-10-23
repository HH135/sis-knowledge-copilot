package io.github.hyun.sis_app_intelligence.collector;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharsetResolverTest {

    @Test
    void decodesUtf8WithoutBom() {
        CharsetResolver resolver = new CharsetResolver(new SourceProperties());

        CharsetResolver.DecodedContent decoded = resolver.decode(
                "sample.java", "한글".getBytes(StandardCharsets.UTF_8));

        assertEquals("한글", decoded.content());
        assertEquals("UTF-8", decoded.charsetName());
    }

    @Test
    void detectsAndRemovesUtf8Bom() {
        CharsetResolver resolver = new CharsetResolver(new SourceProperties());
        byte[] text = "한글".getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[text.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(text, 0, bytes, 3, text.length);

        CharsetResolver.DecodedContent decoded = resolver.decode("sample.java", bytes);

        assertEquals("한글", decoded.content());
        assertEquals("UTF-8", decoded.charsetName());
    }

    @Test
    void fallsBackToMs949ForLegacyKoreanText() {
        SourceProperties properties = new SourceProperties();
        CharsetResolver resolver = new CharsetResolver(properties);

        CharsetResolver.DecodedContent decoded = resolver.decode(
                "legacy.java", "한글".getBytes(Charset.forName("MS949")));

        assertEquals("한글", decoded.content());
        assertEquals("MS949", decoded.charsetName());
    }

    @Test
    void configuredOverrideTakesPrecedence() {
        SourceProperties properties = new SourceProperties();
        properties.setCharsetOverrides(Map.of("legacy.java", "MS949"));
        CharsetResolver resolver = new CharsetResolver(properties);

        CharsetResolver.DecodedContent decoded = resolver.decode(
                "legacy.java", "한글".getBytes(Charset.forName("MS949")));

        assertEquals("한글", decoded.content());
        assertEquals("MS949", decoded.charsetName());
    }
}
