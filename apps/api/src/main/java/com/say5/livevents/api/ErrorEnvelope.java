package com.say5.livevents.api;

/**
 * Stable error envelope shared by every JSON error response. {@code retryable}
 * lets a client distinguish between transient (e.g. 503) and permanent
 * (e.g. 400) failures without parsing the HTTP status.
 */
public record ErrorEnvelope(String code, String message, boolean retryable, String requestId) {
}
