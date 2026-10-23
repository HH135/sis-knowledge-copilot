package io.github.hyun.sis_app_intelligence.dependency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecCallParserTest {

    private final ExecCallParser parser = new ExecCallParser();

    @Test
    void parsesSupportedExecFormsAndDefaultsSchema() {
        var calls = parser.parse("""
                EXEC X;
                EXECUTE dbo.Y @value = 1;
                EXEC [dbo].[Z];
                EXEC @ret = dbo.Returned @value = 2;
                EXECUTE [other].[Brack]]eted];
                """);

        assertEquals(5, calls.size());
        assertEquals(new ParsedCall("dbo", "X", "PROCEDURE", 1), calls.get(0));
        assertEquals(new ParsedCall("dbo", "Y", "PROCEDURE", 2), calls.get(1));
        assertEquals(new ParsedCall("dbo", "Z", "PROCEDURE", 3), calls.get(2));
        assertEquals(new ParsedCall("dbo", "Returned", "PROCEDURE", 4), calls.get(3));
        assertEquals(new ParsedCall("other", "Brack]eted", "PROCEDURE", 5), calls.get(4));
    }

    @Test
    void ignoresLineBlockCommentsAndStringLiteralsWhileKeepingLineNumbers() {
        var calls = parser.parse("""
                -- EXEC dbo.Commented;
                SELECT 'EXEC dbo.StringValue', 'it''s EXEC fake';
                /* EXEC dbo.Blocked;
                   EXEC dbo.AlsoBlocked; */
                EXEC dbo.RealCall;
                """);

        assertEquals(1, calls.size());
        assertEquals(new ParsedCall("dbo", "RealCall", "PROCEDURE", 5), calls.getFirst());
    }

    @Test
    void detectsDynamicAndSystemCalls() {
        var calls = parser.parse("""
                EXEC sys.sp_executesql @sql;
                EXEC (@sql);
                EXECUTE @dynamicName;
                EXEC dbo.sp_help;
                """);

        assertEquals(4, calls.size());
        assertEquals(new ParsedCall("sys", "sp_executesql", "DYNAMIC", 1), calls.get(0));
        assertEquals(new ParsedCall("dbo", "(dynamic)", "DYNAMIC", 2), calls.get(1));
        assertEquals(new ParsedCall("dbo", "(dynamic)", "DYNAMIC", 3), calls.get(2));
        assertEquals(new ParsedCall("dbo", "sp_help", "SYSTEM", 4), calls.get(3));
    }
}
