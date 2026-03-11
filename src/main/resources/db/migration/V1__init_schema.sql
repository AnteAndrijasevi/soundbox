-- Users
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(500),
    bio           TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Artists
CREATE TABLE artists
(
    id        BIGSERIAL PRIMARY KEY,
    mbid      VARCHAR(36)  NOT NULL UNIQUE,
    name      VARCHAR(255) NOT NULL,
    bio       TEXT,
    image_url VARCHAR(500)
);

-- Albums
CREATE TABLE albums
(
    id              BIGSERIAL PRIMARY KEY,
    mbid            VARCHAR(36)  NOT NULL UNIQUE,
    title           VARCHAR(255) NOT NULL,
    artist_id       BIGINT REFERENCES artists (id) ON DELETE CASCADE,
    release_date    DATE,
    cover_art_url   VARCHAR(500),
    tracklist       TEXT,
    last_fetched_at TIMESTAMP
);

-- Album genres (@ElementCollection)
CREATE TABLE album_genres
(
    album_id BIGINT      NOT NULL REFERENCES albums (id) ON DELETE CASCADE,
    genre    VARCHAR(100) NOT NULL
);

-- Reviews
CREATE TABLE reviews
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    album_id   BIGINT         NOT NULL REFERENCES albums (id) ON DELETE CASCADE,
    rating     NUMERIC(3, 1)  NOT NULL CHECK (rating >= 0.5 AND rating <= 5.0),
    text       TEXT,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reviews_user_album UNIQUE (user_id, album_id)
);

-- Listen logs
CREATE TABLE listen_logs
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    album_id    BIGINT    NOT NULL REFERENCES albums (id) ON DELETE CASCADE,
    listened_at TIMESTAMP NOT NULL DEFAULT NOW(),
    rating      NUMERIC(3, 1) CHECK (rating >= 0.5 AND rating <= 5.0)
);

-- Lists
CREATE TABLE lists
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_public   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- List items
CREATE TABLE list_items
(
    id       BIGSERIAL PRIMARY KEY,
    list_id  BIGINT  NOT NULL REFERENCES lists (id) ON DELETE CASCADE,
    album_id BIGINT  NOT NULL REFERENCES albums (id) ON DELETE CASCADE,
    position INTEGER,
    note     TEXT
);

-- Follows
CREATE TABLE follows
(
    follower_id  BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    following_id BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id)
);

-- Likes
CREATE TABLE likes
(
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    review_id  BIGINT    NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, review_id)
);
