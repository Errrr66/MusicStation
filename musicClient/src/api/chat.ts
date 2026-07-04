import { httpGet, httpPost, getAuthToken } from '@/utils/http'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AgentNowPlaying {
  songId?: string
  title?: string
  artist?: string
  album?: string
}

export interface AgentSongCard {
  songId?: number | null
  songName: string
  artistName: string
  album?: string
  coverUrl?: string
  audioUrl?: string
  source?: string
  reason?: string
}

export interface AgentPlaylistCard {
  playlistId?: number | null
  title: string
  coverUrl?: string
  source?: string
  reason?: string
  songCount?: number
  tracks?: AgentSongCard[]
}

export interface AgentPlaylistSaveResult {
  playlistId: number
  title: string
  songCount: number
}

export interface AgentToolTrace {
  tool: string
  status: string
  summary: string
}

export interface AgentCitation {
  sourceType: string
  sourceId: string
  title: string
  snippet: string
  reason: string
}

export interface AgentChatResponse {
  answer: string
  audio: string
  intent: string
  playerCommand: string
  toolTrace: AgentToolTrace[]
  songs: AgentSongCard[]
  playlists: AgentPlaylistCard[]
  citations?: AgentCitation[]
  musicArchive: Record<string, any>
}

export interface ChatHealthResponse {
  ragEnabled: boolean
  ragMode: string
  ragLastRetrieval: {
    strategy: string
    mode: string
    queryCount: number
    candidateCount: number
    citationCount: number
    enabled: boolean
    updatedAtEpochMs: number
  }
  providers: {
    deepseekConfigured: boolean
    ttsConfigured: boolean
  }
  serverTime: string
}

export const sendChatMessage = (messages: ChatMessage[]) => {
  return httpPost<{ code: number; message: string; data: { answer: string; audio: string } }>(
    '/chat/ask',
    { messages }
  )
}

export const sendAgentMessage = (payload: {
  message?: string
  messages?: ChatMessage[]
  nowPlaying?: AgentNowPlaying
  limit?: number
  enableVoice?: boolean
  enableRag?: boolean
  playlistSeeds?: Array<{ songId: number; songName: string; artistName: string; style?: string }>
}) => {
  return httpPost<{ code: number; message: string; data: AgentChatResponse }>('/chat/agent', payload)
}

export const sendAgentMessageStream = async (
  payload: {
    message?: string
    messages?: ChatMessage[]
    nowPlaying?: AgentNowPlaying
    limit?: number
    enableVoice?: boolean
    enableRag?: boolean
    playlistSeeds?: Array<{ songId: number; songName: string; artistName: string; style?: string }>
  },
  handlers: {
    onDelta: (chunk: string) => void
    onDone: (data: AgentChatResponse) => void
    onError?: (message: string) => void
  },
  options?: {
    signal?: AbortSignal
  }
) => {
  const token = getAuthToken()

  const response = await fetch(`${import.meta.env.VITE_APP_BASE_API}/chat/agent/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: token } : {}),
    },
    body: JSON.stringify(payload),
    signal: options?.signal,
  })

  if (!response.ok || !response.body) {
    // 错误对象携带 status 与 body，便于调用方区分 401/403/500 等情况
    const bodyText = await response.text().catch(() => '')
    const error = new Error(`Stream request failed: ${response.status}`)
    ;(error as Error & { status: number }).status = response.status
    ;(error as Error & { body: string }).body = bodyText
    throw error
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const parseSseBlock = (block: string) => {
    const lines = block
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)

    const eventLine = lines.find((line) => line.startsWith('event:'))
    const dataLines = lines
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s?/, ''))

    if (!eventLine || dataLines.length === 0) {
      return
    }

    const event = eventLine.replace('event:', '').trim()
    const data = dataLines.join('\n').trim()

    if (event === 'delta') {
      handlers.onDelta(data)
      return
    }

    if (event === 'done') {
      try {
        handlers.onDone(JSON.parse(data) as AgentChatResponse)
      } catch {
        handlers.onError?.('Stream done payload parse failed')
      }
      return
    }

    if (event === 'error') {
      handlers.onError?.(data || 'Stream error')
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })

    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    blocks.forEach(parseSseBlock)
  }
}

export const saveAgentPlaylist = (payload: {
  title: string
  introduction?: string
  style?: string
  coverUrl?: string
  tracks: Array<{ songId?: number | null; songName: string; artistName: string }>
}) => {
  return httpPost<{ code: number; message: string; data: AgentPlaylistSaveResult }>('/chat/agent/savePlaylist', payload)
}

export const getChatHealth = () => {
  return httpGet<{ code: number; message: string; data: ChatHealthResponse }>('/chat/health')
}

