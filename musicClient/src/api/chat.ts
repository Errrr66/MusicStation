import { httpPost } from '@/utils/http'

export const sendChatMessage = (message: string) => {
  return httpPost<{ code: number; message: string; data: string }>(
    '/chat/ask',
    { message }
  )
}
