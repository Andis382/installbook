package io.github.andis382.installbook.auth;

/** What a person may do inside their organisation. */
public enum Role {
    /** The installer who owns the business: everything, including settings and team. */
    OWNER,
    /** A helper who records installs and visits; no settings, no team management. */
    TECHNICIAN
}
