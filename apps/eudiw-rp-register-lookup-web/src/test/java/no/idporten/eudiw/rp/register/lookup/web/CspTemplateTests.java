package no.idporten.eudiw.rp.register.lookup.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CspTemplateTests {

    private static final Pattern INLINE_STYLE = Pattern.compile("\\sstyle\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_SCRIPT = Pattern.compile(
        "(?is)<script\\b(?![^>]*\\b(?:th:)?src\\s*=)[^>]*>");

    private final ResourcePatternResolver resourcePatternResolver =
        ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader());

    @Test
    void templatesDoNotUseInlineStylesOrScripts() throws IOException {
        Resource[] templates = resourcePatternResolver.getResources("classpath*:/templates/**/*.html");

        assertTrue(templates.length > 0, "Expected HTML templates to be found");
        for (Resource template : templates) {
            String html;
            try (var input = template.getInputStream()) {
                html = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            }
            assertFalse(INLINE_STYLE.matcher(html).find(),
                () -> "Inline style found in " + template.getFilename());
            assertFalse(INLINE_SCRIPT.matcher(html).find(),
                () -> "Inline script found in " + template.getFilename());
        }
    }
}
