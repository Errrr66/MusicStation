import { httpPost } from '@/utils/http'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export const sendChatMessage = (messages: ChatMessage[]) => {
  return httpPost<{ code: number; message: string; data: { answer: string; audio: string } }>(
    '/chat/ask',
    { messages }
  )
}
