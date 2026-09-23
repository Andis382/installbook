package io.github.andis382.installbook.common;

import io.github.andis382.installbook.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** HMAC-signed, expiring links so public pages can show private photos without a login. */
@Component
public class SignedUrls {

    private final AppProperties props;
    private final Clock clock;

    public SignedUrls(AppProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    public String fileUrl(String fileId, Duration ttl) {
        long exp = clock.instant().plus(ttl).getEpochSecond();
        return "/api/public/files/" + fileId + "?exp=" + exp + "&sig=" + sign(fileId + ":" + exp);
    }

    public boolean verify(String fileId, long exp, String sig) {
        if (exp < clock.instant().getEpochSecond() || sig == null) {
            return false;
        }
        byte[] expected = sign(fileId + ":" + exp).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, sig.getBytes(StandardCharsets.US_ASCII));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign URL", e);
        }
    }
}
