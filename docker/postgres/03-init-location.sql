CREATE DATABASE location;
GRANT ALL PRIVILEGES ON DATABASE location TO platform;
\c location;

CREATE TABLE countries (
    id   INTEGER      PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    iso3 CHAR(3),
    iso2 CHAR(2)
);

CREATE TABLE states (
    id         INTEGER      PRIMARY KEY,
    country_id INTEGER      NOT NULL REFERENCES countries(id),
    name       VARCHAR(100) NOT NULL,
    state_code VARCHAR(10)
);

CREATE TABLE cities (
    id       INTEGER      PRIMARY KEY,
    state_id INTEGER      NOT NULL REFERENCES states(id),
    name     VARCHAR(100) NOT NULL
);

CREATE TABLE postal_codes (
    id          INTEGER     PRIMARY KEY,
    country_id  INTEGER     NOT NULL REFERENCES countries(id),
    state_id    INTEGER     NOT NULL REFERENCES states(id),
    city_id     INTEGER     NOT NULL REFERENCES cities(id),
    postal_code VARCHAR(20) NOT NULL
);

INSERT INTO countries (id, name, iso3, iso2) VALUES
    (1, 'Azerbaijan', 'AZE', 'AZ'),
    (2, 'Turkey',     'TUR', 'TR');

INSERT INTO states (id, country_id, name, state_code) VALUES
    (10, 1, 'Baku',       'BA'),
    (11, 1, 'Ganja-Dash', 'GD'),
    (20, 2, 'Istanbul',   'IST');

INSERT INTO cities (id, state_id, name) VALUES
    (100, 10, 'Baku'),
    (101, 10, 'Khirdalan'),
    (110, 11, 'Ganja'),
    (200, 20, 'Istanbul');

INSERT INTO postal_codes (id, country_id, state_id, city_id, postal_code) VALUES
    (1000, 1, 10, 100, 'AZ1000'),
    (1001, 1, 10, 100, 'AZ1001'),
    (1002, 1, 10, 101, 'AZ0101'),
    (1100, 1, 11, 110, 'AZ2000'),
    (2000, 2, 20, 200, 'TR34000'),
    (2001, 2, 20, 200, 'TR34010');
