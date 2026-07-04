import type { PersistedStateOptions } from 'pinia-plugin-persistedstate'

/**
 * @description pinia 持久化参数配置
 * @param {String} key 存储到持久化的 name
 * @return persist
 * */
const piniaPersistConfig = (key: string) => {
  const persist: PersistedStateOptions = {
    key,
    storage: localStorage,
    // storage: sessionStorage,
  }
  return persist
}

export default piniaPersistConfig

/**
 * 安全提示：
 * 当前 token 与 userInfo 一同持久化到 localStorage，存在 XSS 窃取风险。
 * 生产环境建议：
 * 1. 将 token 改为 httpOnly cookie（由后端设置），前端不再持有；
 * 2. 或至少使用 sessionStorage 并配合短期过期策略；
 * 3. 敏感字段（token）应通过自定义 serializer 在持久化前剥离。
 */
