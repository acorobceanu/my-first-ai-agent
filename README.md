# Profession Aptitude API

Spring Boot REST API that uses Spring AI with OpenAI ChatGPT prompting to run a session-based career aptitude interview. The service asks up to 15 questions, but stops earlier when the model has enough evidence for a strong profession recommendation.

## Requirements

- Java 21
- Maven 3.9+
- `OPENAI_API_KEY` environment variable

## Run

```bash
export OPENAI_API_KEY=your-api-key
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

H2 console:

```text
http://localhost:8080/h2-console
```

Use JDBC URL `jdbc:h2:mem:aptitude`, username `sa`, and an empty password.

## API Flow

Create a session:

```bash
curl -X POST http://localhost:8080/api/v1/aptitude/sessions
```

Submit an answer:

```bash
curl -X POST http://localhost:8080/api/v1/aptitude/sessions/{sessionId}/answers \
  -H 'Content-Type: application/json' \
  -d '{"answer":"I enjoy solving ambiguous problems, organizing teams, and communicating tradeoffs."}'
```

Get a session:

```bash
curl http://localhost:8080/api/v1/aptitude/sessions/{sessionId}
```

## Configuration

```yaml
spring.ai.openai.api-key: ${OPENAI_API_KEY}
spring.ai.openai.chat.options.model: ${OPENAI_CHAT_MODEL:gpt-4o-mini}
aptitude.max-questions: 15
```

Spring AI OpenAI docs reference the `spring-ai-starter-model-openai` starter and `spring.ai.openai.*` configuration. The app uses Spring AI `1.1.7`, which was announced as the current stable release line in May 2026.
