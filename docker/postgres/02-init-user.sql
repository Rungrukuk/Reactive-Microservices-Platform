CREATE DATABASE "user";
GRANT ALL PRIVILEGES ON DATABASE "user" TO platform;
\c "user";


CREATE TABLE users (
    id           VARCHAR(36)  PRIMARY KEY,
    name         VARCHAR(100),
    surname      VARCHAR(100),
    phone_number VARCHAR(20)
);

CREATE TABLE addresses (
    id             BIGSERIAL    PRIMARY KEY,
    country        VARCHAR(100),
    state          VARCHAR(100),
    city           VARCHAR(100),
    postal_code    VARCHAR(20),
    address_line_1 TEXT,
    address_line_2 TEXT
);

CREATE TABLE user_addresses (
    id         VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id    VARCHAR(36) NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    address_id BIGINT      NOT NULL REFERENCES addresses(id) ON DELETE CASCADE,
    is_default BOOLEAN     NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX ux_user_default_address
ON user_addresses (user_id)
WHERE is_default = true;
