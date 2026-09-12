-- Allow login by phone for any User (customers and staff).
ALTER TABLE `user`
    ADD COLUMN phone VARCHAR(15) UNIQUE;
