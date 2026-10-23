package io.github.hyun.sis_app_intelligence.document;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperStatementChunkerTest {

    private final MapperStatementChunker chunker = new MapperStatementChunker();

    @Test
    void extractsEachStatementWithNamespaceIdAndPathReference() {
        String xml = """
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://example.invalid/mybatis.dtd">
                <mapper namespace="com.acme.RepairDao">
                    <select id="find">select 1</select>
                    <insert id="save">insert into t values (1)</insert>
                    <update id="change">update t set x = 2</update>
                    <delete id="remove">delete from t</delete>
                </mapper>
                """;

        var chunks = chunker.chunk(input("portfolio-data/app/resources/mapper/RepairMapper.xml", xml));

        assertEquals(4, chunks.size());
        assertEquals("com.acme.RepairDao.find", chunks.get(0).title());
        assertEquals("portfolio-data/app/resources/mapper/RepairMapper.xml#find", chunks.get(0).sourceRef());
        assertEquals(1, chunks.get(0).startLine());
    }

    @Test
    void doesNotResolveExternalEntitiesOrExternalDtd() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = "external-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/external.dtd";
            String xml = "<!DOCTYPE mapper SYSTEM \"" + url + "\" ["
                    + "<!ENTITY injected SYSTEM \"" + url + "\">]>"
                    + "<mapper namespace=\"safe\"><select id=\"query\">&injected;</select></mapper>";

            var chunks = chunker.chunk(input("mapper/Mapper.xml", xml));

            assertEquals(1, chunks.size());
            assertFalse(chunks.getFirst().content().contains("external-secret"));
            assertEquals(0, requests.get());
        }
        finally {
            server.stop(0);
        }
    }

    @Test
    void parsesPortfolioMapperWithStandardMyBatisDoctype() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.acme.Sample">
                    <select id="one">select 1</select>
                </mapper>
                """;

        var chunks = chunker.chunk(input("mapper/Sample.xml", xml));

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().content().contains("select 1"));
    }

    private ChunkingInput input(String path, String xml) {
        return new ChunkingInput("portfolio", "MAPPER_STATEMENT", path, path, xml, null, null);
    }
}
