# Changelog - 2026-04-06

## Final Optimization Delivery

### Backend

- Added RAG hybrid retrieval skeleton in `AgentRagService`
  - Supports `keyword`, `vector`, and `hybrid` modes
  - Added weighted scoring controls
  - Tracks last retrieval health metrics
- Added `GET /chat/health`
  - Exposes RAG status and provider readiness
  - Returns retrieval counters and update timestamp
- Improved streaming controller runtime handling
  - Replaced unbounded cached pool with fixed thread pool for SSE tasks
  - Replaced console output with structured logging in `ChatController`
- Removed NetEase OpenAPI health/config dependency from chat health pipeline
- Renamed lyric helper in `SongServiceImpl`
  - `fetchLyricFromNetease` -> `fetchLyricFromProvider`
  - No behavior change

### Frontend

- Enhanced chat UX in `musicClient/src/pages/chat/index.vue`
  - Added health refresh and collapsible health panel
  - Added stream status visibility and latency metrics
  - Added retrieval hit indicator
- Added clickable citation behavior
  - Playlist citations route to playlist detail
  - Song citations trigger play or library search fallback
- Updated API typing in `musicClient/src/api/chat.ts`
  - Added chat health response model
  - Added citation typing support

### Configuration

- Added RAG options in `application.yml`
  - `agent.rag.mode`
  - `agent.rag.keyword-weight`
  - `agent.rag.vector-weight`
- Removed `netease.openapi.*` block from active config

## Verification Performed

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicServer"
mvnw.cmd -q -DskipTests compile
```

```bat
cd /d "E:\IntelliJ IDEA\workspace\music\musicClient"
pnpm type-check
```

## Notes

- This changelog focuses on agent/rag/chat delivery.
- Existing project historical changelog remains unchanged.

