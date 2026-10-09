# CodeConverse

Chat with any of your GitHub repositories. CodeConverse indexes a repo's source code into a vector database and uses Retrieval-Augmented Generation (RAG) to answer natural-language questions about it, with citations to the exact files used.

## Features

- **GitHub OAuth2 login** and per-user repository listing
- **Async indexing pipeline**: fetch repo tree, filter files, chunk code, embed, store
- **Semantic search** over code using pgvector (HNSW index, cosine distance), scoped per repository
- **Streaming answers** over Server-Sent Events (SSE) with source-file citations
- **Persistent chat sessions** and message history
- **Secure by design**: GitHub access tokens encrypted at rest, secrets loaded from environment, HTTP-only session cookies

## How it works

```
GitHub repo ──► File filter ──► Chunker ──► Embedding model ──► pgvector
                                              (Jina)              │
Question ──► Embed question ──► Top-8 similar chunks (repo-scoped) ┘
                                      │
                       Prompt = system rules + retrieved code + question
                                      │
                               Gemini (chat) ──► SSE stream ──► UI + citations
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring AI, Spring Security (OAuth2), Spring Data JPA |
| Database | PostgreSQL 16 + pgvector |
| LLM / Embeddings | Google Gemini (chat), Jina AI `jina-embeddings-v3`, 1024 dims (embeddings) |
| Frontend | Next.js, React, Tailwind CSS, shadcn/ui |
| Infra | Docker Compose |

## Getting started

### Prerequisites

- JDK 21, Maven, Node.js 18+, Docker
- A [GitHub OAuth App](https://github.com/settings/developers) with callback URL `http://localhost:8080/login/oauth2/code/github`
- A [Gemini API key](https://aistudio.google.com/) and a [Jina API key](https://jina.ai/embeddings/) (both have free tiers)

### 1. Start the database

```bash
docker compose up -d
```

PostgreSQL with pgvector runs on `localhost:5433`.

### 2. Configure secrets

Never commit real keys. Provide them as environment variables, or in `backend/src/main/resources/application-local.properties` (git-ignored) and run with the `local` profile.

| Variable | Description |
|---|---|
| `GEMINI_API_KEY` | Gemini API key |
| `JINA_API_KEY` | Jina API key |
| `GITHUB_CLIENT_ID` | GitHub OAuth app client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app client secret |
| `TOKEN_ENCRYPTED_PASSWORD` | Any long random string (encrypts stored GitHub tokens) |
| `TOKEN_ENCRYPTOR_SALT` | Hex string, e.g. 32 hex characters |

Optional: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`.

### 3. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`, sign in with GitHub, pick a repository, click **Index**, then start chatting.

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/auth/login-url` | GitHub login URL |
| GET | `/api/auth/me` | Current user |
| GET | `/api/repos` | List user's repositories |
| POST | `/api/repos/{id}/index` | Start indexing a repository |
| GET | `/api/repos/{id}/status` | Indexing progress |
| POST | `/api/chat/sessions` | Create a chat session |
| GET | `/api/chat/sessions` | List chat sessions |
| GET | `/api/chat/sessions/{id}` | Session with messages |
| POST | `/api/chat/sessions/{id}/messages` | Ask a question (SSE stream) |

## Project structure

```
backend/src/main/java/.../backend
├── config/        Security, CORS, async executor, token encryption, Jina embedding model
├── controllers/   Auth, repository, chat endpoints
├── services/
│   ├── github/    GitHub API client + rate limiter
│   ├── indexing/  File filter, chunker, indexing service
│   └── ai/        Retriever, prompt builder, streaming handler, citations
├── entity/ repository/ dto/ security/ exceptions/
frontend/          Next.js app (dashboard + chat UI)
docker-compose.yml PostgreSQL + pgvector
```

## Design notes

- **Custom Jina embedding client.** Spring AI's OpenAI-compatible client requires `usage.prompt_tokens` in embedding responses, which Jina doesn't return. `JinaEmbeddingModel` calls Jina's REST API directly, with batching (32) and exponential backoff on HTTP 429, and is registered as the primary `EmbeddingModel`.
- **Repo-scoped retrieval.** Every chunk is stored with a `repoId` metadata field, and similarity search filters on it so answers never leak across repositories.
- **Re-indexing is idempotent.** Existing vectors for a repo are deleted before indexing starts.

## Limitations

- Free-tier rate limits (Gemini, Jina) slow indexing of large repositories.
- Changing the embedding model or dimensions requires dropping the `vector_store` table and re-indexing.
