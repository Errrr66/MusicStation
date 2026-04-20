import { useNavigatorLanguage } from '@vueuse/core'

/**
 * 获取浏览器语言
 * @returns {string} 当前浏览器的语言代码
 */
export function getBrowserLang() {
  const { language } = useNavigatorLanguage()
  if (['zh-CN'].includes(language.value as string)) {
    language.value = 'zh'
  }
  return language.value
}

export function formatTime(seconds: number): string {
  // 将秒数转换为整数分钟数和剩余秒数
  const min = Math.floor(seconds / 60)
  const sec = Math.floor(seconds % 60)

  // 返回格式化的字符串，确保分钟和秒数都至少有两位数
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

export function formatMillisecondsToTime(totalMilliseconds: number) {
  const totalSeconds = Math.floor(totalMilliseconds / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds - hours * 3600) / 60)
  const seconds = totalSeconds % 60

  return [hours, minutes, seconds]
    .map((n) => (n < 10 ? `0${n}` : n.toString()))
    .filter((val, index) => val !== '00' || index > 0) // 移除开头的"00"小时
    .join(':')
}

/**
 * @description 获取当前时间对应的提示语
 * @returns {String}
 */
export function getTimeState() {
  let timeNow = new Date()
  let hours = timeNow.getHours()
  if (hours >= 6 && hours <= 10) return `早上好 ⛅`
  if (hours >= 10 && hours <= 14) return `中午好 🌞`
  if (hours >= 14 && hours <= 18) return `下午好 🌞`
  if (hours >= 18 && hours <= 24) return `晚上好 🌛`
  if (hours >= 0 && hours <= 6) return `凌晨好 🌛`
}

/**
 * 修复 MinIO URL 在局域网访问时的问题
 * 将 localhost 或 127.0.0.1 替换为当前访问的主机名
 */
export function fixUrl(url: string | undefined | null): string {
  if (!url) return ''
  const normalizedUrl = url.trim()
  if (!normalizedUrl) return ''

  // 相对路径直接返回，保持与历史行为一致
  if (normalizedUrl.startsWith('/')) {
    return normalizedUrl
  }

  // 签名 URL 不能做任何重写或编码处理，否则签名会失效
  const signedUrlPattern = /(x-amz-|x-oss-|signature=|awsaccesskeyid=)/i
  if (signedUrlPattern.test(normalizedUrl)) {
    return normalizedUrl
  }

  try {
    const currentHost = window.location.hostname
    return normalizedUrl.replace(/localhost|127\.0\.0\.1/g, currentHost)
  } catch (e) {
    return normalizedUrl
  }
}

/**
 * 为图片 URL 安全追加 MinIO 缩略参数。
 * - 自动处理 ? / & 拼接
 * - 已存在 param 时会覆盖，避免重复参数导致服务端解析异常
 */
export function appendImageParam(
  url: string | undefined | null,
  size: string = '200y200'
): string {
  const normalizedUrl = fixUrl(url)
  if (!normalizedUrl) return ''
  if (!size) return normalizedUrl

  // blob/data URL 不能追加 query
  const lower = normalizedUrl.toLowerCase()
  if (lower.startsWith('blob:') || lower.startsWith('data:')) {
    return normalizedUrl
  }

  const signedQueryPattern = /(x-amz-|x-oss-|signature=|awsaccesskeyid=)/i
  if (signedQueryPattern.test(normalizedUrl)) {
    return normalizedUrl
  }

  const [urlWithoutHash, hash = ''] = normalizedUrl.split('#')
  const hashSuffix = hash ? `#${hash}` : ''
  const questionIndex = urlWithoutHash.indexOf('?')

  if (questionIndex === -1) {
    return `${urlWithoutHash}?param=${size}${hashSuffix}`
  }

  const query = urlWithoutHash.slice(questionIndex + 1)
  if (signedQueryPattern.test(query)) {
    return `${urlWithoutHash}${hashSuffix}`
  }

  // 已存在 param 时保持原样
  if (/(^|&)param=/.test(query)) {
    return `${urlWithoutHash}${hashSuffix}`
  }

  return `${urlWithoutHash}&param=${size}${hashSuffix}`
}

export function formatNumber(num: number): string {
  if (num >= 1e8) {
    return (num / 1e8).toFixed(0) + '亿' // 1亿
  } else if (num >= 1e4) {
    return (num / 1e4).toFixed(0) + 'W' // 1万
  } else {
    return num.toString() // 小于1万的直接返回
  }
}

export function replaceUrlParams(url: string, newParam: string): string {
  // 找到第一个出现问号的位置
  const questionMarkIndex = url.indexOf('?')

  // 如果没有找到问号，直接返回原始的 URL
  if (questionMarkIndex === -1) {
    return url
  }

  // 提取问号之前的部分（即基本 URL）
  const baseUrl = url.substring(0, questionMarkIndex)

  // 生成新的 URL，替换参数
  return `${baseUrl}?${newParam}`
}

export function parseTimestamp(timestamp: string): string {
  // 创建一个 Date 对象
  const date = new Date(timestamp)

  // 获取年、月、日、时、分、秒
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0') // 月份从0开始，所以要加1
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  // 返回格式化后的日期字符串
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}
