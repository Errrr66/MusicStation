import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosRequestHeaders,
} from 'axios'
import NProgress from '@/config/nprogress'
import 'nprogress/nprogress.css'
import { UserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'

type HttpMeta = {
  silentProgress?: boolean
  silentError?: boolean
}

type HttpRequestConfig = Omit<AxiosRequestConfig, 'method' | 'url'> & {
  meta?: HttpMeta
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API, // 从环境变量读取接口地址
  timeout: 20000, // 设置超时时间 20秒
  headers: {
    Accept: 'application/json, text/plain, */*',
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
  withCredentials: false,
})

// 请求拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const meta = (config as InternalAxiosRequestConfig & { meta?: HttpMeta }).meta
    // 开启进度条
    if (!meta?.silentProgress) {
      NProgress.start()
    }

    // 只有登录请求不需要添加token
    if (config.url?.includes('/user/login')) {
      return config
    }

    // 从 pinia 中获取token
    const userStore = UserStore()
    const token = userStore.userInfo?.token

    if (token) {
      // 确保headers对象存在并且是正确的类型
      if (!config.headers) {
        config.headers = {} as AxiosRequestHeaders
      }
      // 添加Bearer前缀
      config.headers.Authorization = token
    }

    // console.log('请求URL:', config.url)
    // console.log('请求头:', config.headers)
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    const meta = (response.config as AxiosRequestConfig & { meta?: HttpMeta }).meta
    // 关闭进度条
    if (!meta?.silentProgress) {
      NProgress.done()
    }
    const { data } = response
    return data
  },
  (error) => {
    const meta = (error.config as AxiosRequestConfig & { meta?: HttpMeta } | undefined)?.meta
    // 关闭进度条
    if (!meta?.silentProgress) {
      NProgress.done()
    }

    if (meta?.silentError) {
      return Promise.reject(error)
    }

    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 如果不是登录请求，则清除用户信息
          if (!error.config.url?.includes('/user/login')) {
            const userStore = UserStore()
            userStore.clearUserInfo()
            ElMessage.error('登录已过期，请重新登录')
          } else {
            ElMessage.error('邮箱或密码错误')
          }
          break
        case 403:
          ElMessage.error('没有权限')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error('网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }

    return Promise.reject(error)
  }
)

// 封装request方法
export const http = <T>(
  method: 'get' | 'post' | 'put' | 'delete' | 'patch',
  url: string,
  config?: HttpRequestConfig
): Promise<T> => {
  return instance({ method, url, ...config })
}

// 封装get方法
export const httpGet = <T>(url: string, params?: object): Promise<T> =>
  instance.get(url, { params })

// 封装post方法
export const httpPost = <T>(
  url: string,
  data?: object,
  header?: object
): Promise<T> => instance.post(url, data, { headers: header })

// 封装upload方法
export const httpUpload = <T>(
  url: string,
  formData: FormData,
  header?: object
): Promise<T> => {
  return instance.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...header,
    },
  })
}
