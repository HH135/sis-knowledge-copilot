package io.github.hyun.sis_app_intelligence.document;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Component
public class MapperStatementChunker implements ChunkingStrategy {

    @Override
    public List<ChunkDraft> chunk(ChunkingInput input) {
        try {
            var document = secureFactory().newDocumentBuilder().parse(new InputSource(new StringReader(input.content())));
            Element mapper = document.getDocumentElement();
            String namespace = mapper.getAttribute("namespace");
            List<ChunkDraft> chunks = new ArrayList<>();
            collectStatements(mapper, namespace, input, chunks);
            return List.copyOf(chunks);
        }
        catch (Exception exception) {
            throw new MapperParseException("Mapper XML could not be parsed.", exception);
        }
    }

    private void collectStatements(Element parent, String namespace, ChunkingInput input, List<ChunkDraft> chunks) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) {
                continue;
            }
            String tag = localName(element);
            if ("select".equals(tag) || "insert".equals(tag) || "update".equals(tag) || "delete".equals(tag)) {
                String id = element.getAttribute("id");
                String title = namespace + "." + id;
                String content = element.getTextContent().trim();
                chunks.add(new ChunkDraft(
                        input.origin(),
                        "MAPPER_STATEMENT",
                        input.sourceRef() + "#" + id,
                        title,
                        1,
                        1,
                        content,
                        LineWindowChunker.sha256(content)));
            }
            else {
                collectStatements(element, namespace, input, chunks);
            }
        }
    }

    private String localName(Element element) {
        String name = element.getLocalName();
        return name == null ? element.getTagName() : name;
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
        factory.setExpandEntityReferences(false);
        return factory;
    }

    public static class MapperParseException extends RuntimeException {
        public MapperParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
