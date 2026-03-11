import { httpGet } from '@/utils/http'

// 获取音乐连接
export const urlV1 = (id: number | string) => {
  const audio = AudioStore()
  return httpGet<{ data: { url: string }[] }>(
    `song/url/v1?id=${id}&level=${audio.quality}`
  )
}