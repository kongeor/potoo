CREATE TABLE users
(
    id SERIAL PRIMARY KEY NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200),
    created_at timestamptz NOT NULL DEFAULT now()
);
;;--
CREATE TABLE potoos
(
    user_id int references users(id),
    text VARCHAR(140) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
)