package io.github.andis382.installbook.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Every API error has the same shape: {"message": "...", "errors": {"field": ["..."]}}.
 * The web client renders "errors" next to fields and in a linked summary.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new java.util.ArrayList<>())
                .add(messages.getMessage(fe, LocaleContextHolder.getLocale()));
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
            errors.computeIfAbsent("form", k -> new java.util.ArrayList<>())
                .add(messages.getMessage(ge, LocaleContextHolder.getLocale())));
        return body(HttpStatus.UNPROCESSABLE_CONTENT, text("error.validation"), errors);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getFieldErrors().forEach((field, keys) ->
            errors.put(field, keys.stream().map(k -> text(k, ex.getArgs())).toList()));
        return body(ex.getStatus(), text(ex.getMessage(), ex.getArgs()), errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, Object>> badCredentials() {
        String msg = text("auth.failed");
        return body(HttpStatus.UNPROCESSABLE_CONTENT, msg, Map.of("email", List.of(msg)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> denied() {
        return body(HttpStatus.FORBIDDEN, text("error.forbidden"), Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> unreadable(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, text("error.bad_request"), Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> tooLarge() {
        return body(HttpStatus.CONTENT_TOO_LARGE, text("error.file_too_large"), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> noResource() {
        return body(HttpStatus.NOT_FOUND, text("error.not_found"), Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, text("error.server"), Map.of());
    }

    private String text(String key, Object... args) {
        return messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Map<String, List<String>> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        if (!errors.isEmpty()) {
            body.put("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
