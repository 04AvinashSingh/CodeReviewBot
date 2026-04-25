# 🤖 AI Code Review Bot

> **Production-ready, multi-tenant SaaS** — A GitHub App that automatically reviews Pull Requests using Google Gemini AI and posts inline comments.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://docs.docker.com/compose/)

---

## 📐 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        GITHUB                                   │
│                                                                 │
│   Developer opens PR ──► GitHub sends webhook ──────────┐       │
│                                                         │       │
│   PR gets inline comments ◄── GitHub REST API ◄──┐      │       │
└──────────────────────────────────────────────────│──────│────────┘
                                                   │      │
┌──────────────────────────────────────────────────│──────│────────┐
│                   CODE REVIEW BOT                │      │        │
│                                                  │      ▼        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   Auth API   │    │ GitHub Svc   │◄───│  Webhook     │       │
│  │  (JWT+RBAC)  │    │ (REST Client)│    │  Controller  │       │
│  └──────┬───────┘    └──────────────┘    └──────┬───────┘       │
│         │                                       │               │
│  ┌──────▼───────┐    ┌──────────────┐    ┌──────▼───────┐       │
│  │  Tenant API  │    │ Gemini Svc   │◄───│ Review Svc   │       │
│  │ Repos/Usage  │    │  (AI Engine) │    │ (Async/Pool) │       │
│  └──────┬───────┘    └──────────────┘    └──────┬───────┘       │
│         │                                       │               │
│  ┌──────▼───────────────────────────────────────▼───────┐       │
│  │              PostgreSQL (Multi-Tenant)                │       │
│  │   tenants │ repos │ reviews │ comments │ usage        │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps)
- A [Google Gemini API key](https://aistudio.google.com/apikey)

### 1. Clone & Configure

```bash
git clone https://github.com/your-username/code-review-bot.git
cd code-review-bot

# Copy env template and fill in your secrets
cp .env.example .env
```

Edit `.env` with your values:
```env
GITHUB_APP_ID=123456
GITHUB_WEBHOOK_SECRET=your-webhook-secret
GEMINI_API_KEY=your-gemini-key
JWT_SECRET=a-strong-secret-at-least-32-characters
```

### 2. Add GitHub Private Key

Place your GitHub App's private key file at the project root:
```bash
cp ~/Downloads/your-app.private-key.pem ./github-private-key.pem
```

### 3. Start with Docker Compose

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`.

### 4. Set Up Webhook URL

For local development, use [smee.io](https://smee.io) or [ngrok](https://ngrok.com):

```bash
# Option A: smee.io (recommended for dev)
npx smee -u https://smee.io/your-channel -t http://localhost:8080/api/webhooks/github

# Option B: ngrok
ngrok http 8080
```

Set the webhook URL in your GitHub App settings to:
```
https://your-url/api/webhooks/github
```

---

## 📡 API Reference

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | Create tenant account | ❌ |
| `POST` | `/api/auth/login` | Login, get JWT | ❌ |
| `GET` | `/api/auth/me` | Current user info | ✅ |

### Repos

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/repos` | List registered repos | ✅ |
| `POST` | `/api/repos/register` | Register a repo | ✅ |
| `PATCH` | `/api/repos/{id}/toggle` | Enable/disable repo | ✅ |

### Reviews

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/reviews` | List reviews (paginated) | ✅ |
| `GET` | `/api/reviews/{id}` | Review detail + comments | ✅ |

### Usage

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/usage` | Current month stats | ✅ |
| `GET` | `/api/usage/history` | Last 6 months | ✅ |

### Webhooks

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/webhooks/github` | GitHub webhook receiver | HMAC |
| `GET` | `/api/webhooks/github` | Health check | ❌ |

### Swagger UI

Interactive API docs available at: `http://localhost:8080/swagger-ui.html`

---

## 🔧 Usage Examples

### Register & Login

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"securepass123","githubOrgOrUser":"my-org"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"securepass123"}'
```

### Register a Repo

```bash
curl -X POST http://localhost:8080/api/repos/register \
  -H "Authorization: Bearer <your-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"repoFullName":"owner/repo","githubInstallationId":12345678}'
```

### Check Usage

```bash
curl http://localhost:8080/api/usage \
  -H "Authorization: Bearer <your-jwt>"
```

---

## 🏗️ GitHub App Setup

1. Go to **GitHub Settings** → **Developer Settings** → **GitHub Apps** → **New GitHub App**
2. Set:
   - **Homepage URL**: `https://your-domain.com`
   - **Webhook URL**: `https://your-domain.com/api/webhooks/github`
   - **Webhook Secret**: Generate a strong secret
3. **Permissions**:
   - `Pull requests`: Read & Write
   - `Contents`: Read
4. **Events**: Subscribe to `Pull request`
5. Generate a **Private Key** and download the `.pem` file
6. Note the **App ID** from the app settings page
7. **Install** the app on your org/repos

---

## 📊 Database Schema

```
tenants ──────────< repos ──────────< reviews ──────────< review_comments
    │                                     
    └────────────< usage_tracking          
```

- **tenants**: Multi-tenant accounts with plan (FREE/PRO) and RBAC roles
- **repos**: Registered repositories linked to tenants via installation IDs
- **reviews**: PR review records with status tracking and token usage
- **review_comments**: Individual inline comments from AI reviews
- **usage_tracking**: Per-tenant per-month review and token counters

---

## ⚙️ Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `GITHUB_APP_ID` | GitHub App ID | required |
| `GITHUB_PRIVATE_KEY_PATH` | Path to .pem file | required |
| `GITHUB_WEBHOOK_SECRET` | Webhook HMAC secret | required |
| `GEMINI_API_KEY` | Google Gemini API key | required |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | required |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/codereviewbot` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `PORT` | Server port | `8080` |

---

## 🚢 Deployment

### Railway

1. Connect your GitHub repo to [Railway](https://railway.app)
2. Add a PostgreSQL service
3. Set all environment variables from the table above
4. Railway will auto-detect the Dockerfile and deploy

### Render

1. Create a **Web Service** on [Render](https://render.com)
2. Connect your repo, select **Docker** environment
3. Add a **PostgreSQL** database
4. Set environment variables, use the internal DB URL
5. Deploy

---

## 📁 Project Structure

```
src/main/java/com/codereviewbot/
├── config/          SecurityConfig, AsyncConfig, GeminiConfig
├── controller/      WebhookController, AuthController, RepoController,
│                    ReviewController, UsageController
├── dto/             Request/response DTOs
├── entity/          JPA entities + enums
├── exception/       GlobalExceptionHandler, custom exceptions
├── repository/      Spring Data JPA repositories
├── security/        JwtUtil, JwtAuthenticationFilter, TenantPrincipal
└── service/         GitHubService, GeminiService, ReviewService,
                     TenantService, UsageTrackingService
```

---

## 📋 Plans & Rate Limits

| Plan | Reviews/Month | Price |
|------|--------------|-------|
| FREE | 50 | $0 |
| PRO | Unlimited | Contact |

---

## License

MIT
