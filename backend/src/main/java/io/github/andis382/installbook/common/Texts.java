package io.github.andis382.installbook.common;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Looks up translated strings from messages*.properties. */
@Component
public class Texts {

    private final MessageSource messages;

    public Texts(MessageSource messages) {
        this.messages = messages;
    }

    /** In the language of the current request. */
    public String get(String key, Object... args) {
        return messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    /** In an explicit language, e.g. the customer's, when writing to them. */
    public String in(String locale, String key, Object... args) {
        return messages.getMessage(key, args, key, Locale.forLanguageTag(locale == null ? "sq" : locale));
    }
}
