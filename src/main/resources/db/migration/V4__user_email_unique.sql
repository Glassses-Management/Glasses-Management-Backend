-- Enforce unique emails on the User account table (no duplicate login emails).
ALTER TABLE "user"
    ADD CONSTRAINT uk_user_email UNIQUE (email);
