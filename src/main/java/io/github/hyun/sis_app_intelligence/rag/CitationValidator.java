package io.github.hyun.sis_app_intelligence.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class CitationValidator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)]");

    public ValidationResult validate(String answer, List<RetrievedChunk> contexts) {
        if (answer == null) {
            return new ValidationResult(null, 0, List.of());
        }
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        StringBuffer sanitized = new StringBuffer();
        int invalid = 0;
        Set<Integer> validNumbers = new LinkedHashSet<>();
        while (matcher.find()) {
            int number;
            try {
                number = Integer.parseInt(matcher.group(1));
            }
            catch (NumberFormatException exception) {
                invalid++;
                matcher.appendReplacement(sanitized, "");
                continue;
            }
            if (number < 1 || number > contexts.size()) {
                invalid++;
                matcher.appendReplacement(sanitized, "");
            }
            else {
                validNumbers.add(number);
                matcher.appendReplacement(sanitized, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sanitized);
        List<CitedContext> citations = new ArrayList<>();
        for (int number : validNumbers) {
            RetrievedChunk context = contexts.get(number - 1);
            citations.add(new CitedContext(number, context));
        }
        return new ValidationResult(sanitized.toString(), invalid, List.copyOf(citations));
    }

    public record CitedContext(int no, RetrievedChunk chunk) {
    }

    public record ValidationResult(String answer, int invalidCitations, List<CitedContext> citations) {
    }
}
