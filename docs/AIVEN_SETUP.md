# Aiven setup

The database is **PostgreSQL, hosted on Aiven**. This guide covers creating the
service, pointing the app at it, and the two things that go wrong most often:
TLS, and Flyway's behaviour on a database that already has data.

`docs/RAILWAY_DEPLOYMENT.md` describes the previous MySQL-on-Railway setup and is
kept only for reference. It no longer matches the code.

---

## 1. Create the service

1. Sign in to [console.aiven.io](https://console.aiven.io).
2. **Create service** → **PostgreSQL**.
3. Pick a region close to where the app runs. The free plan is single-region.
4. Choose a plan. The free plan is enough for development but suspends the
   service after a period of inactivity, and it is the tier that expires, which
   is why this moved off Render.
5. Under **Connection settings**, set the database name to `glasses_management`
   and leave the generated user as-is.

Aiven shows a connection string when the service is ready:

```
postgresql://avn_userXXXXXXXX:XXXXXXXX@pg-XXXXXXXX-XXXX.aivencloud.com:5432/glasses_management?sslmode=require
```

## 2. Point the app at it

Copy `.env.example` to `.env` (it is gitignored) and fill in the five database
values from that connection string:

```properties
DB_HOST=pg-XXXXXXXX-XXXX.aivencloud.com
DB_PORT=5432
DB_NAME=glasses_management
DB_USERNAME=avn_userXXXXXXXX
DB_PASSWORD=XXXXXXXX
DB_SSLMODE=require
```

**Aiven refuses plaintext connections.** `DB_SSLMODE=require` is the safe value.
The application default is `prefer`, which negotiates TLS when the server offers
it and falls back to plaintext when it does not; that single setting also works
against a local database. Set `disable` only for a server with no TLS at all.

The JDBC URL is assembled from those values in
`src/main/resources/application.properties`:

```
jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSLMODE}
```

Do not paste the whole `postgresql://` URI into `DB_HOST` — `DB_HOST` is the
hostname only. (If you prefer to keep one value, set `SPRING_DATASOURCE_URL`
instead and leave the `DB_*` keys alone; Spring Boot lets the explicit URL win.)

## 3. Migrations: which mode you need

Flyway's job is to create the schema, but this database may already have one.
`application.properties` therefore defaults to the safe option for a database
that **already has data**: it baselines at V10, so V1–V10 are treated as applied
and no migration tries to recreate an existing table.

| Situation | What to do |
|---|---|
| Database already has data (moved from MySQL) | Nothing. The default is already correct. |
| Database exists but has no `flyway_schema_history` | Nothing. The default baselines it at V10. |
| Database is completely empty | Start once with the `local` profile so the migrations run. |

For the empty case:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`application-local.properties` exists for exactly this: it sets
`spring.flyway.baseline-on-migrate=false` so Flyway runs V1–V10 instead of
skipping them. The `app` service in `docker-compose.yml` sets the profile
already. Once the schema exists, drop the profile and start normally.

You only need the `local` profile once. If you later drop and recreate the
database, use it again.

## 4. Moving existing data across

The schema and history both travel with a dump, so the default profile is right
afterwards. Two routes, depending on where the data is now.

### From MySQL, with pgloader

pgloader converts MySQL to PostgreSQL directly and copies the data:

```bash
pgloader "mysql://USER:PASSWORD@MYSQL_HOST:3306/glasses_management" \
         "postgresql://avn_userXXXXXXXX:XXXXXXXX@pg-XXXXXXXX.aivencloud.com:5432/glasses_management"
```

pgloader copies `flyway_schema_history` along with everything else, so V1–V10
stay recorded as applied and the default profile is correct.

Two things to check afterwards, because pgloader's type mapping is not always
what Hibernate expects:

```sql
-- Is the login table still called "user", and does it have the phone column?
SELECT table_name FROM information_schema.tables
 WHERE table_schema = 'public' ORDER BY table_name;

-- FLOAT columns: Hibernate expects REAL (4-byte), not DOUBLE PRECISION.
SELECT table_name, column_name, data_type FROM information_schema.columns
 WHERE table_schema = 'public'
   AND data_type IN ('real', 'double precision')
 ORDER BY table_name, column_name;
```

If pgloader produced `double precision` where `real` is expected, the app will
refuse to start with a schema-validation error. Fix it with:

```sql
ALTER TABLE prescription ALTER COLUMN od_sphere          TYPE REAL;
ALTER TABLE prescription ALTER COLUMN od_cylinder        TYPE REAL;
ALTER TABLE prescription ALTER COLUMN os_sphere          TYPE REAL;
ALTER TABLE prescription ALTER COLUMN os_cylinder        TYPE REAL;
ALTER TABLE prescription ALTER COLUMN near_addition      TYPE REAL;
ALTER TABLE prescription ALTER COLUMN pupillary_distance TYPE REAL;
ALTER TABLE order_item  ALTER COLUMN len_index           TYPE REAL;
```

### From another PostgreSQL (e.g. the old Render database)

```bash
pg_dump --format=custom --no-owner --file=backup.dump \
  "postgresql://USER:PASSWORD@OLD_HOST:5432/glasses_management"

pg_restore --dbname="postgresql://avn_userXXXXXXXX:XXXXXXXX@pg-XXXXXXXX.aivencloud.com:5432/glasses_management" \
  --no-owner --clean --if-exists backup.dump
```

`--clean --if-exists` drops the existing objects first. Drop `--clean` to merge
into a database that already holds something.

## 5. Checksum validation is off

`application.properties` sets:

```properties
spring.flyway.validate-on-migrate=false
```

This is deliberate and temporary. V1–V10 were originally written for MySQL and
have since been rewritten as PostgreSQL, so their checksums no longer match the
ones recorded in any `flyway_schema_history` that travelled with the data.
Leaving validation on would make the app refuse to start.

The cost is that Flyway will no longer warn you if you edit a migration that has
already been applied. Turn it back on once you have realigned the history:

```bash
./mvnw flyway:repair -Dflyway.url="jdbc:postgresql://HOST:5432/glasses_management?sslmode=require" \
                   -Dflyway.user=avn_userXXXXXXXX -Dflyway.password=XXXXXXXX
```

`repair` recomputes the recorded checksums from the current files. It only writes
to `flyway_schema_history`; no application data is touched. Afterwards, set
`spring.flyway.validate-on-migrate=true` and leave it that way.

## 6. Troubleshooting

**`The connection attempt failed ... SSL error`** — `DB_SSLMODE` is not set, or
is set to `disable`. Aiven requires `require`.

**`FATAL: password authentication failed for user`** — the credentials are wrong,
or they were rotated in the Aiven console after you copied them. Re-copy.

**`could not translate host name`** — the whole `postgresql://...` URI was pasted
into `DB_HOST`. Use the hostname only.

**`Schema-validation: wrong column type encountered in column [x] ... found
[double precision (Types#DOUBLE]), but expecting [real (Types#REAL)]`** — the
pgloader type mapping from section 4. Apply the `ALTER` statements listed there.

**`relation "user" does not exist`** — the table is there but unquoted, and
`user` is a reserved word. The app already sets
`spring.jpa.properties.hibernate.globally_quoted_identifiers=true`, which quotes
every identifier, so this should not happen; if it does, the table was renamed
to something else and `@Table(name = "user")` in `User.java` must match.

**`Table ... not found` right after migrating** — the schema was created but the
app was started with the default profile, which skips migrations. Create the
database empty and start it once with the `local` profile instead.

**Aiven says the connection limit was reached** — lower the pool size, e.g.
`spring.datasource.hikari.maximum-pool-size=5`. The default is 10, and the free
plan allows 20.
