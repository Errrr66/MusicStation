import { httpPost } from '@/utils/http'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export const sendChatMessage = (messages: ChatMessage[]) => {
  return httpPost<{ code: number; message: string; data: string }>(
    '/chat/ask',
    { messages }
  )
}
