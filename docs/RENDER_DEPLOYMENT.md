# Render deployment

The backend runs on [Render](https://render.com) as a Docker web service, talking
to the Aiven PostgreSQL database. Configuration lives in `render.yaml` (a Render
Blueprint), so the service is created from the repository rather than clicked
together by hand.

`docs/RAILWAY_DEPLOYMENT.md` describes the old MySQL-on-Railway setup and is kept
only for reference.

---

## What Render does with this repository

- Builds `./Dockerfile` (two stages: Maven build, then a JRE runtime).
- Starts `java -jar app.jar`, which reads `server.port=${PORT:8080}`. Render
  provides `PORT`, so the app binds to it automatically.
- Sets `SPRING_PROFILES_ACTIVE=production` (from the Dockerfile), which picks up
  `application-production.properties` and turns off SQL logging and security
  DEBUG logs.
- Checks `GET /api/products` for liveness. It is public and hits the database, so
  it fails if Aiven is unreachable.

## Steps

1. Push this repo to GitHub (including `render.yaml`).
2. Render dashboard → **New** → **Blueprint** → select the repo.
3. Render reads `render.yaml` and shows the service plus a list of secret
   variables. Fill in each `sync: false` value:

   | Variable | Where it comes from |
   |---|---|
   | `DB_PASSWORD` | Aiven service → Connection information |
   | `JWT_SECRET` | Generate: `openssl rand -base64 32` |
   | `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Choose these; used only to seed the first admin |
   | `APP_BASE_URL` | `https://<service-name>.onrender.com` (Render shows it) |
   | `GOOGLE_CLIENT_ID` | Google Cloud Console, or leave blank |
   | `GOOGLE_ALLOWED_STAFF_EMAILS` | Comma-separated staff emails, or leave blank |
   | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Cloudinary dashboard |

4. **Apply**. The first build takes a few minutes.

`APP_BASE_URL` is what the QR code and `/request-form` link point at, so it must
be the Render URL, scheme included. If it is wrong, scanning the printed QR lands
on the wrong host.

## Environment variables

Non-secret values are already in `render.yaml`. The full set the app reads is in
`.env.example`. Two groups are easy to get wrong:

- **`DB_*`** — Aiven's **external** connection details. Render is not on Aiven's
  private network, so the internal hostname will not resolve. `DB_SSLMODE=require`
  is mandatory; Aiven rejects plaintext.
- **Cloudinary** — there is no default. `CloudinaryStorageService` takes them as
  constructor arguments, so a missing value fails startup with
  `Could not resolve placeholder 'CLOUDINARY_CLOUD_NAME'`.

## Migrations on a fresh deploy

Nothing to do. `application.properties` baselines at V10 and does not validate
checksums, because the schema and its `flyway_schema_history` already arrived from
MySQL. Flyway logs `Schema "glasses_management" is up to date. No migration
necessary.` and leaves the data alone.

Do **not** set `SPRING_PROFILES_ACTIVE=local` on Render. That profile disables
baselining so the migrations run, which is only correct for an empty database; on
a database that already has tables it would try to recreate them and fail.

## Free tier caveats

- **Cold starts.** A free service sleeps after ~15 minutes of inactivity. The
  next request waits roughly 30–60 seconds for the JVM to start. Fine for a demo,
  awkward if customers scan the QR after an idle period.
- **512 MB RAM.** The app fits, but with little headroom. Keep
  `spring.jpa.show-sql=false` (the `production` profile does).
- **One free database per account.** The database is on Aiven, so this is not a
  problem here.

## QR code

`/api/qr/request` returns a PNG encoding `APP_BASE_URL/request-form`, generated on
the fly. `/api/qr/current` returns the same thing, falling back to a dynamic
image because no tunnel QR file exists. The printable dashboard is at
`/qr-dashboard`.

## Troubleshooting

**Build fails with `Could not resolve placeholder 'CLOUDINARY_...`** — a required
Cloudinary variable is not set. Add it and redeploy.

**`FATAL: password authentication failed` or `could not translate host name`** —
`DB_*` values are wrong, or the Aiven hostname was used instead of Render's
external one. Re-copy from the Aiven console.

**Service deploys but the health check fails** — read the logs. The most common
cause is the SSL error: `DB_SSLMODE` missing or set to `disable`.

**Deploy succeeds, QR points at localhost** — `APP_BASE_URL` is unset or still
the default. Set it to the Render URL and redeploy.

**Aiven: connection limit reached** — the free plan allows 20 connections. Lower
the pool with `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5`.

**Rotating the database password** — after rotating in Aiven, update
`DB_PASSWORD` in the Render dashboard. The value in `.env` is local only.
