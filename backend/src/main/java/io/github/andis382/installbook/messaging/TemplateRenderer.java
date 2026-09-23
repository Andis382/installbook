package io.github.andis382.installbook.messaging;

import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Customer-facing message texts live in messages*.properties under "msg.&lt;key&gt;" with named
 * placeholders: msg.warranty_card=Hi {customer}, your {unit} ... {link}.
 */
@Component
public class TemplateRenderer {

    private final MessageSource messages;

    public TemplateRenderer(MessageSource messages) {
        this.messages = messages;
    }

    public String render(String key, String locale, Map<String, String> params) {
        Locale loc = Locale.forLanguageTag(locale == null ? "sq" : locale);
        // With no arguments Spring returns the raw pattern, so apostrophes need no escaping here.
        String text = messages.getMessage("msg." + key, null, key, loc);
        for (Map.Entry<String, String> e : params.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return text;
    }
}
