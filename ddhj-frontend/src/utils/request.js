import axios from 'axios'
import { showToast } from 'vant'

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 60000
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            showToast(res.message || '请求失败')
            return Promise.reject(new Error(res.message || '请求失败'))
        }
        return res.data
    },
    error => {
        showToast(error.message || '网络错误')
        return Promise.reject(error)
    }
)

export default request
