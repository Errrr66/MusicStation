# AI Agent Runbook

This runbook describes the final AI Agent architecture, runtime checks, and troubleshooting steps for the `music` workspace.

## Scope

- Backend: `musicServer`
- Frontend: `musicClient`
- Focus: Agent chat, RAG retrieval, SSE streaming, TTS, and health visibility

## 1. Current Architecture

### 1.1 Request Flow

1. Client sends request to `POST /chat/agent` or `POST /chat/agent/stream`
2. Backend detects intent in `MusicAgentService`
3. Optional RAG retrieval runs in `AgentRagService`
4. Retrieved context is appended to prompt
5. LLM generates final response text
6. Optional TTS generates audio URL
7. Response returns with `toolTrace`, `songs`, `playlists`, and `citations`

### 1.2 RAG Modes

Configured in `musicServer/src/main/resources/application.yml`:

- `keyword`: keyword-only ranking
- `vector`: lightweight vector approximation ranking
- `hybrid`: weighted keyword + vector ranking (default)

### 1.3 Health Endpoint

`GET /chat/health`

Returns:

- RAG switch and mode
- Last retrieval stats (`queryCount`, `candidateCount`, `citationCount`)
- Provider readiness (`deepseekConfigured`, `ttsConfigured`)

No secrets are returned by this endpoint.

## 2. Key Configuration

`musicServer/src/main/resources/application.yml`

- `deepseek.api-key`
- `deepseek.base-url`
- `deepseek.model`
- `tts.api-url`
- `agent.search.*`
- `agent.rag.enabled`
- `agent.rag.top-k`
- `agent.rag.max-snippet-length`
- `agent.rag.mode`
- `agent.rag.keyword-weight`
- `agent.rag.vector-weight`

## 3. Frontend UX Features

In `musicClient/src/pages/chat/index.vue`:

- Live stream status bar (`connecting`, `streaming`, `done`, `error`)
- First-token and total latency display
- Collapsible health panel with RAG/provider state
- Citation cards with click actions:
  - `playlist` -> route to `/playlist/:id`
  - `song` -> play matched item or jump to library search

## 4. API Contract Highlights

### 4.1 Agent Request

`AgentChatRequestDTO` supports:

- `message`
- `messages`
- `nowPlaying`
- `enableVoice`
- `enableRag`
- `limit`

### 4.2 Agent Response

`AgentChatResponseVO` includes:

- `answer`
- `audio`
- `intent`
- `playerCommand`
- `toolTrace`
- `songs`
- `playlists`
- `citations`
- `musicArchive`

## 5. Validation Commands

Run from Windows `cmd.exe`.

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile
```

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm type-check
pnpm build
```

## 6. Troubleshooting

### 6.1 No citations returned

- Check `/chat/health` -> `ragEnabled`
- Lower query complexity and retry
- Verify local data has matching songs/playlists

### 6.2 Stream interrupted

- Use stop/resume controls in chat UI
- Check browser network stream for `event: error`
- Retry with non-stream endpoint `POST /chat/agent`

### 6.3 LLM unavailable

- Check `deepseekConfigured` in `/chat/health`
- Validate `deepseek.api-key` and `deepseek.base-url`

### 6.4 TTS missing audio

- Check `ttsConfigured` in `/chat/health`
- Verify `tts.api-url` service health

## 7. Operational Notes

- Keep secrets in environment variables; avoid hard-coding in repo files.
- Keep `/chat/health` exposed only as needed by your deployment policy.
- Prefer `hybrid` mode for balanced relevance and robustness.

