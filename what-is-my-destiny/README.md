# What Is My Destiny?

React UI for the `my-ai-agent` aptitude interview API. The app creates a backend session, submits free-text answers with that session id, and renders the final recap and recommendations returned by the REST API.

## Stack

- Vite
- React
- TypeScript
- Material UI

## Run Locally

Install dependencies:

```bash
npm install
```

Start the backend from the repository root:

```bash
mvn spring-boot:run
```

Start the UI:

```bash
npm run dev
```

The default `VITE_API_BASE_URL` is `/api/v1`. Vite proxies `/api` to `http://localhost:8080`, so the UI can call the Spring Boot backend during local development without extra CORS configuration.

To use a different backend origin, create `.env.local`:

```bash
VITE_BACKEND_ORIGIN=http://localhost:8080
VITE_API_BASE_URL=/api/v1
```

## API Flow

- `POST /api/v1/aptitude/sessions`
- `POST /api/v1/aptitude/sessions/{sessionId}/answers`
- final findings are rendered from the returned session payload when `status` is `COMPLETED`
