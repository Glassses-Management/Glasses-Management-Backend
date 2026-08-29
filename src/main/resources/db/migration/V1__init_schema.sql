-- Flyway baseline schema for the Optical Shop Management backend.
-- Mirrors the JPA entities (source of truth). Money columns use DECIMAL(19,2).
-- Fresh databases only: Flyway creates these tables on app startup.

CREATE TABLE `user` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255),
    email         VARCHAR(255),
    role          VARCHAR(20),
    password_hash VARCHAR(255),
    created_at    DATETIME,
    updated_at    DATETIME
);

CREATE TABLE customer (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(20)  NOT NULL,
    phone        VARCHAR(15)  NOT NULL UNIQUE,
    email        VARCHAR(100) NOT NULL UNIQUE,
    address      VARCHAR(100),
    date_of_birth DATE,
    created_at   DATETIME,
    updated_at   DATETIME
);

CREATE TABLE product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku              VARCHAR(255),
    model            VARCHAR(255),
    brand            VARCHAR(255),
    color            VARCHAR(255),
    material         VARCHAR(255),
    size             VARCHAR(255),
    cost_price       DECIMAL(19, 2),
    sale_price       DECIMAL(19, 2),
    supplier_name    VARCHAR(255),
    supplier_contact VARCHAR(255),
    category         VARCHAR(255),
    created_at       DATETIME,
    updated_at       DATETIME
);

CREATE TABLE inventory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT NOT NULL,
    quantity            INT   NOT NULL,
    reorder_threshold   INT   NOT NULL,
    last_restocked_at   DATETIME,
    created_at          DATETIME,
    updated_at          DATETIME,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE appointment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    optometrist_id BIGINT,
    scheduled_at  DATETIME NOT NULL,
    status        VARCHAR(20) NOT NULL,
    notes         TEXT,
    created_at    DATETIME,
    CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_appointment_optometrist FOREIGN KEY (optometrist_id) REFERENCES `user` (id)
);

CREATE TABLE prescription (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id        BIGINT NOT NULL,
    user_id            BIGINT,
    od_sphere          FLOAT,
    od_cylinder        FLOAT,
    od_axis            INT,
    os_sphere          FLOAT,
    os_cylinder        FLOAT,
    os_axis            INT,
    near_addition      FLOAT,
    pupillary_distance FLOAT,
    notes              VARCHAR(255),
    prescription_date  DATE,
    created_at         DATETIME,
    updated_at         DATETIME,
    CONSTRAINT fk_prescription_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_prescription_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    user_id         BIGINT,
    prescription_id BIGINT,
    total           DECIMAL(19, 2),
    status          VARCHAR(20),
    payment_method  VARCHAR(30),
    payment_status  VARCHAR(20),
    order_date      DATE,
    created_at      DATETIME,
    updated_at      DATETIME,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_orders_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id)
);

CREATE TABLE order_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    quantity     INT   NOT NULL,
    len_type     VARCHAR(50),
    len_coating  VARCHAR(50),
    len_index    FLOAT,
    len_price    DECIMAL(19, 2),
    unit_price   DECIMAL(19, 2),
    total_price  DECIMAL(19, 2),
    created_at   DATETIME,
    updated_at   DATETIME,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);
