-- Notifications: built by the Kafka consumer when someone you follow logs a listen.
CREATE TABLE notifications
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_username VARCHAR(50) NOT NULL,
    album_mbid   VARCHAR(36),
    album_title  VARCHAR(255),
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
