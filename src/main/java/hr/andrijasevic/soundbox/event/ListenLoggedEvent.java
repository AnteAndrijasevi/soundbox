package hr.andrijasevic.soundbox.event;

/**
 * Emitted when a user logs a listen. Consumed asynchronously to fan out notifications
 * to the user's followers (and a natural seam for future feed/analytics consumers).
 * Kept to primitive/String fields so it serializes cleanly over Kafka JSON.
 */
public record ListenLoggedEvent(
        Long userId,
        String username,
        String albumMbid,
        String albumTitle
) {
    public static final String TOPIC = "soundbox.listen-logged";
}
