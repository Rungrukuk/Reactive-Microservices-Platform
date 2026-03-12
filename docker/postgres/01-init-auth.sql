\c auth;

CREATE TABLE roles (
    role_name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    service     VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL
);

CREATE TABLE role_permissions (
    role_name     VARCHAR(50) NOT NULL REFERENCES roles(role_name) ON DELETE CASCADE,
    permission_id BIGINT      NOT NULL REFERENCES permissions(id)  ON DELETE CASCADE,
    PRIMARY KEY (role_name, permission_id)
);

CREATE TABLE users (
    user_id   VARCHAR(36)  PRIMARY KEY,
    email     VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    role_name VARCHAR(50)  NOT NULL REFERENCES roles(role_name)
);

CREATE TABLE refresh_tokens (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id       VARCHAR(36)  NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    refresh_token TEXT         NOT NULL,
    user_agent    VARCHAR(255),
    client_city   VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_device UNIQUE (user_id, user_agent, client_city)
);

INSERT INTO roles (role_name) VALUES ('GUEST_USER'), ('USER');

INSERT INTO permissions (id, service, destination) VALUES
    (1, 'NONE',         'NONE'),
    (2, 'AUTH_SERVICE', 'REGISTER'),
    (3, 'AUTH_SERVICE', 'LOGIN'),
    (4, 'USER_SERVICE', 'CREATE_USER_DETAILS');

INSERT INTO role_permissions (role_name, permission_id) VALUES
    ('GUEST_USER', 1),
    ('GUEST_USER', 2),
    ('GUEST_USER', 3),
    ('USER',       1),
    ('USER',       4);
