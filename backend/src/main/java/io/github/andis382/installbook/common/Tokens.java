package io.github.andis382.installbook.common;

import java.security.SecureRandom;
import java.util.Base64;

/** Unguessable tokens for public links (warranty cards, parent links, quote pages...). */
public final class Tokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private Tokens() {}

    /** 22 URL-safe characters, 128 bits of entropy. */
    public static String urlToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Short human-readable code without look-alike characters (no 0/O, 1/I). */
    public static String shortCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    /** Numeric one-time code, e.g. for consent confirmation. */
    public static String digits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
