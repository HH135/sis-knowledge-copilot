package io.github.hyun.sis_app_intelligence.dependency;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Component
public class MapperCallParser {

    private static final List<String> STATEMENT_TAGS = List.of("select", "insert", "update", "delete");
    private static final Pattern JDBC_CALL_PREFIX = Pattern.compile("(?i)\\{\\s*call\\b");

    private final ExecCallParser execCallParser;

    public MapperCallParser(ExecCallParser execCallParser) {
        this.execCallParser = execCallParser;
    }

    public List<MapperCall> parse(String path, String xml) {
        try {
            var document = secureFactory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            List<MapperCall> calls = new ArrayList<>();
            collectStatements(document.getDocumentElement(), path, calls);
            return List.copyOf(calls);
        }
        catch (Exception exception) {
            throw new MapperParseException("Mapper XML could not be parsed.", exception);
        }
    }

    private void collectStatements(Element parent, String path, List<MapperCall> calls) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) {
                continue;
            }
            String tagName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
            if (STATEMENT_TAGS.contains(tagName)) {
                String statementId = element.getAttribute("id");
                String callerRef = path + "#" + statementId;
                String statement = JDBC_CALL_PREFIX.matcher(element.getTextContent()).replaceAll("EXEC");
                for (ParsedCall call : execCallParser.parse(statement)) {
                    calls.add(new MapperCall(callerRef, call));
                }
            }
            else {
                collectStatements(element, path, calls);
            }
        }
    }

    private DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    public record MapperCall(String callerRef, ParsedCall parsedCall) {
    }

    public static class MapperParseException extends RuntimeException {
        public MapperParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
