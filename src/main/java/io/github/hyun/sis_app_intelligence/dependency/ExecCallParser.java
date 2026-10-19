package io.github.hyun.sis_app_intelligence.dependency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ExecCallParser {

    private static final Pattern EXEC_KEYWORD = Pattern.compile("(?i)\\bEXEC(?:UTE)?\\b");
    private static final Pattern IDENTIFIER = Pattern.compile("(\\[[^\\]]+(?:\\]\\][^\\]]*)*\\]|[\\w$#]+)");
    private static final Pattern ASSIGNMENT = Pattern.compile("^@[\\w$#]+\\s*=\\s*");

    public List<ParsedCall> parse(String sql) {
        String code = maskCommentsAndStrings(sql == null ? "" : sql);
        Matcher matcher = EXEC_KEYWORD.matcher(code);
        List<ParsedCall> calls = new ArrayList<>();
        while (matcher.find()) {
            ParsedCall call = parseCall(code, matcher.start(), matcher.end());
            if (call != null) {
                calls.add(call);
            }
        }
        calls.sort(Comparator.comparingInt(ParsedCall::lineNo));
        return List.copyOf(calls);
    }

    private ParsedCall parseCall(String code, int callStart, int afterKeyword) {
        int cursor = skipWhitespace(code, afterKeyword);
        if (cursor >= code.length()) {
            return null;
        }
        if (code.charAt(cursor) == '(') {
            return new ParsedCall("dbo", "(dynamic)", "DYNAMIC", lineAt(code, callStart));
        }

        Matcher assignment = ASSIGNMENT.matcher(code.substring(cursor));
        if (assignment.find()) {
            cursor += assignment.end();
            cursor = skipWhitespace(code, cursor);
        }
        else if (code.charAt(cursor) == '@') {
            return new ParsedCall("dbo", "(dynamic)", "DYNAMIC", lineAt(code, callStart));
        }

        Matcher firstIdentifier = IDENTIFIER.matcher(code);
        firstIdentifier.region(cursor, code.length());
        if (!firstIdentifier.lookingAt()) {
            return null;
        }
        String first = unquote(firstIdentifier.group(1));
        cursor = skipWhitespace(code, firstIdentifier.end());
        String schema = "dbo";
        String name = first;
        if (cursor < code.length() && code.charAt(cursor) == '.') {
            cursor = skipWhitespace(code, cursor + 1);
            Matcher secondIdentifier = IDENTIFIER.matcher(code);
            secondIdentifier.region(cursor, code.length());
            if (!secondIdentifier.lookingAt()) {
                return null;
            }
            schema = first;
            name = unquote(secondIdentifier.group(1));
        }
        String kind = name.equalsIgnoreCase("sp_executesql")
                ? "DYNAMIC"
                : schema.equalsIgnoreCase("sys") || name.toLowerCase().startsWith("sp_")
                        ? "SYSTEM"
                        : "PROCEDURE";
        return new ParsedCall(schema, name, kind, lineAt(code, callStart));
    }

    private String maskCommentsAndStrings(String sql) {
        char[] masked = sql.toCharArray();
        int index = 0;
        while (index < sql.length()) {
            if (sql.charAt(index) == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = maskUntilLineEnd(sql, masked, index);
            }
            else if (sql.charAt(index) == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = maskBlockComment(sql, masked, index);
            }
            else if (sql.charAt(index) == '\'') {
                index = maskString(sql, masked, index);
            }
            else {
                index++;
            }
        }
        return new String(masked);
    }

    private int maskUntilLineEnd(String sql, char[] masked, int start) {
        int index = start;
        while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            masked[index++] = ' ';
        }
        return index;
    }

    private int maskBlockComment(String sql, char[] masked, int start) {
        int depth = 1;
        masked[start] = ' ';
        masked[start + 1] = ' ';
        int index = start + 2;
        while (index < sql.length() && depth > 0) {
            if (index + 1 < sql.length() && sql.charAt(index) == '/' && sql.charAt(index + 1) == '*') {
                masked[index++] = ' ';
                masked[index++] = ' ';
                depth++;
            }
            else if (index + 1 < sql.length() && sql.charAt(index) == '*' && sql.charAt(index + 1) == '/') {
                masked[index++] = ' ';
                masked[index++] = ' ';
                depth--;
            }
            else {
                if (sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
                    masked[index] = ' ';
                }
                index++;
            }
        }
        return index;
    }

    private int maskString(String sql, char[] masked, int start) {
        int index = start;
        masked[index++] = ' ';
        while (index < sql.length()) {
            char value = sql.charAt(index);
            if (value == '\'') {
                masked[index++] = ' ';
                if (index < sql.length() && sql.charAt(index) == '\'') {
                    masked[index++] = ' ';
                }
                else {
                    break;
                }
            }
            else {
                if (value != '\n' && value != '\r') {
                    masked[index] = ' ';
                }
                index++;
            }
        }
        return index;
    }

    private int skipWhitespace(String value, int from) {
        int index = from;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private int lineAt(String sql, int index) {
        int line = 1;
        for (int cursor = 0; cursor < index; cursor++) {
            if (sql.charAt(cursor) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String unquote(String identifier) {
        if (identifier.startsWith("[") && identifier.endsWith("]")) {
            return identifier.substring(1, identifier.length() - 1).replace("]]", "]");
        }
        return identifier;
    }
}
