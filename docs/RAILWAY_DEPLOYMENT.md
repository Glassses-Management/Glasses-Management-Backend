# Railway Deployment Guide

## Overview

This guide explains how to deploy the Optical Shop Management System backend on [Railway](https://railway.app).

Railway provides managed MySQL databases, automatic builds from Docker, and environment variable management.

## Prerequisites

1. A [Railway account](https://railway.app) (free tier available)
2. [Railway CLI](https://cli.railway.app/) (optional, can use web dashboard)
3. GitHub repo connected to Railway

## Step 1: Add Files to Repository

The following files have been added/updated for Railway deployment:

- `railway.toml` — Railway build/deploy configuration
- `Dockerfile` — Updated with `SPRING_PROFILES_ACTIVE=production`
- `src/main/resources/application.properties` — Updated for Railway env vars

## Step 2: Create Railway Project

### Via Web Dashboard:
1. Go to [Railway Dashboard](https://railway.app/dashboard)
2. Click **New Project** → **Deploy from GitHub Repo**
3. Select the `Glasses-Management-Backend` repository
4. Railway will auto-detect the Dockerfile and build

### Via CLI:
```bash
railway login
railway init --dockerfile Dockerfile
railway up
```

## Step 3: Add MySQL Database

1. In the Railway dashboard, go to your project
2. Click **Services** tab → **Add a database** → **MySQL**
3. Railway will automatically create a MySQL database and set these env vars:
   - `DATABASE_URL` — Full JDBC connection string
   - `DB_HOST` — MySQL host
   - `DB_USERNAME` — MySQL username
   - `DB_PASSWORD` — MySQL password
   - `DB_NAME` — Database name

## Step 4: Configure Environment Variables

Set these environment variables in the Railway dashboard (**Settings** → **Variables**):

| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | MySQL JDBC URL (auto-set by Railway MySQL add-on) | Yes |
| `DB_HOST` | MySQL host (auto-set by Railway) | Yes |
| `DB_USERNAME` | MySQL username (auto-set by Railway) | Yes |
| `DB_PASSWORD` | MySQL password (auto-set by Railway) | Yes |
| `DB_NAME` | Database name (auto-set by Railway) | Yes |
| `JWT_SECRET` | Base64-encoded JWT secret key | Yes |
| `JWT_EXPIRATION_MINUTES` | Token expiration in minutes | No (default: 60) |
| `ADMIN_EMAIL` | Admin account email | No |
| `ADMIN_PASSWORD` | Admin account password | No |
| `APP_BASE_URL` | Public base URL (e.g., `https://your-app.trycloudflare.com`) | No |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | Yes (for uploads) |
| `CLOUDINARY_API_KEY` | Cloudinary API key | Yes (for uploads) |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | Yes (for uploads) |
| `TZ` | Timezone | No (default: Asia/Bangkok) |

### Generate JWT_SECRET:
```bash
echo -n "your-secure-secret-key" | base64
```
Or use a random one:
```bash
openssl rand -base64 32
```

## Step 5: Configure Services

### MySQL Add-on:
Railway automatically provisions MySQL. The database is ready when the `MySQL` indicator turns green.

### No additional services needed (qr-watcher is optional).

## Step 6: Deploy

### Via CLI:
```bash
railway deploy
```

### Via Web Dashboard:
1. Push changes to GitHub
2. Railway auto-deploys
3. View logs: **Deployments** tab

## Step 7: Verify Deployment

1. Get the app URL from Railway dashboard (e.g., `https://your-app.up.railways.app`)
2. Test health: `curl https://your-app.up.railways.app/api/products`
3. Check Swagger UI: `https://your-app.up.railways.app/swagger-ui.html`

## Database Migration

On first deploy, Flyway automatically runs all 8 migration scripts:
- `V1__init_schema.sql` — Creates all tables
- `V2__customer_user_link.sql` through `V8__appointment_scheduled_at_nullable.sql` — Schema updates

The `spring.flyway.baseline-on-migrate=true` property ensures smooth migration on existing databases.

## Troubleshooting

### Build fails:
- Check Railway build logs in the dashboard
- Ensure `Dockerfile` is in the project root
- Verify `pom.xml` dependencies are correct

### Database connection fails:
- Verify MySQL add-on is provisioned (green indicator)
- Check `DATABASE_URL` env var matches MySQL credentials
- Run `railway variables` to see all env vars

### App crashes on startup:
- Check `JWT_SECRET` is a valid Base64 string
- Verify `spring.jpa.hibernate.ddl-auto=validate` works with Flyway migrations
- Check logs: `railway logs`

### `spring.config.import` warnings:
- The `.env` file is not present on Railway (expected)
- `optional:` prefix handles this gracefully — no action needed

## Local vs Railway

| Aspect | Local | Railway |
|--------|-------|---------|
| Config file | `.env` | Railway env vars |
| Database | Local MySQL | Railway managed MySQL |
| Build | `./mvnw spring-boot:run` | Docker build |
| Port | 8080 | Railway-assigned |
| `spring.config.import` | `file:.env` | Skipped (optional) |

## qr-watcher Service (Optional)

The `qr-watcher` container (Python + cloudflared) is not deployed on Railway by default. It can be deployed separately or run on your local machine.

To deploy on Railway as a separate service:
1. Create a new Railway project for `scripts/qr-watcher/`
2. Use the existing `scripts/qr-watcher/Dockerfile`
3. Set `TUNNEL_HOST=http://your-app.up.railways.app`
