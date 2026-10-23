package io.github.hyun.sis_app_intelligence.dependency;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperCallParserTest {

    @Test
    void extractsProcedureCallsFromMapperStatementsWithoutResolvingExternalEntities() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = "external".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/mapper.dtd";
            String xml = """
                    <!DOCTYPE mapper SYSTEM "%s" [
                      <!ENTITY remote SYSTEM "%s">
                    ]>
                    <mapper namespace="com.acme.OrderDao">
                      <update id="complete">{ call dbo.USP_COMPLETE_REPAIR_ORDER(1) }</update>
                      <select id="ignored">&remote;</select>
                    </mapper>
                    """.formatted(url, url);

            var calls = new MapperCallParser(new ExecCallParser()).parse("mapper/OrderMapper.xml", xml);

            assertEquals(1, calls.size());
            assertEquals("mapper/OrderMapper.xml#complete", calls.getFirst().callerRef());
            assertEquals("USP_COMPLETE_REPAIR_ORDER", calls.getFirst().parsedCall().name());
            assertEquals(0, requests.get());
        }
        finally {
            server.stop(0);
        }
    }
}
