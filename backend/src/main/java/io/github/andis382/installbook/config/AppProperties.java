package io.github.andis382.installbook.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything the app reads from the environment, in one place.
 * Defaults are safe for local development; production sets the secrets.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Product name used in messages and page titles. */
    private String name = "Kit";
    /** Base URL of the web app, used to build links sent to customers. */
    private String publicUrl = "http://localhost:5173";
    /** Demo mode seeds a sample organisation and enables the message simulator. */
    private boolean demo = true;
    /** Secret for signing public file links. Override in production. */
    private String secret = "dev-secret-change-me";
    private String storageDir = "./storage";
    private String defaultLocale = "sq";
    private String defaultTimezone = "Europe/Tirane";
    private String defaultCountryCode = "355";
    private final Messaging messaging = new Messaging();
    private final Ai ai = new Ai();

    public static class Messaging {
        /** "log" keeps messages in the outbox only; "whatsapp" sends through the Cloud API. */
        private String driver = "log";
        private final WhatsApp whatsapp = new WhatsApp();

        public String getDriver() { return driver; }
        public void setDriver(String driver) { this.driver = driver; }
        public WhatsApp getWhatsapp() { return whatsapp; }
    }

    public static class WhatsApp {
        private String token = "";
        private String phoneNumberId = "";
        private String appSecret = "";
        private String verifyToken = "";
        private String apiVersion = "v23.0";
        /** Maps a template key (e.g. "warranty_card") to an approved WhatsApp template name. */
        private Map<String, String> templates = new HashMap<>();

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public Map<String, String> getTemplates() { return templates; }
        public void setTemplates(Map<String, String> templates) { this.templates = templates; }
    }

    public static class Ai {
        private String apiKey = "";
        private String model = "claude-opus-5";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getStorageDir() { return storageDir; }
    public void setStorageDir(String storageDir) { this.storageDir = storageDir; }
    public String getDefaultLocale() { return defaultLocale; }
    public void setDefaultLocale(String defaultLocale) { this.defaultLocale = defaultLocale; }
    public String getDefaultTimezone() { return defaultTimezone; }
    public void setDefaultTimezone(String defaultTimezone) { this.defaultTimezone = defaultTimezone; }
    public String getDefaultCountryCode() { return defaultCountryCode; }
    public void setDefaultCountryCode(String defaultCountryCode) { this.defaultCountryCode = defaultCountryCode; }
    public Messaging getMessaging() { return messaging; }
    public Ai getAi() { return ai; }

    /** Absolute link into the web app, e.g. link("/c/abc") -> https://host/c/abc. */
    public String link(String path) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
