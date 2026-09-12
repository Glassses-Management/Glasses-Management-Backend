CREATE TABLE attachment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name  VARCHAR(255),
    file_type  VARCHAR(255),
    file_path  VARCHAR(255),
    file_size  BIGINT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE user_attachment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT,
    attachment_id BIGINT,
    created_at    DATETIME,
    CONSTRAINT fk_user_attachment_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_user_attachment_attachment FOREIGN KEY (attachment_id) REFERENCES attachment (id)
);
