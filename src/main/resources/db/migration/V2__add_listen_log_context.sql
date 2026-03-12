ALTER TABLE listen_logs
    ADD COLUMN mood VARCHAR(50),
    ADD COLUMN context VARCHAR(50),
    ADD COLUMN is_first_listen BOOLEAN,
    ADD COLUMN note VARCHAR(500),
    ADD COLUMN favorite_track VARCHAR(255);