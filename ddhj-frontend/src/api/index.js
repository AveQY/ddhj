import request from '@/utils/request'

// 商品相关
export const getProductList = (params) => request.get('/products', { params })
export const getAllProducts = () => request.get('/products/all')
export const getProductById = (id) => request.get(`/products/${id}`)
export const addProduct = (data) => request.post('/products', data)
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/products/${id}`)

// 分类相关
export const getAllCategories = () => request.get('/categories')
export const getCategoryById = (id) => request.get(`/categories/${id}`)
export const addCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)
export const sortCategories = (ids) => request.post('/categories/sort', ids)

// 规格相关
export const getSpecificationsByProductId = (productId) => request.get(`/specifications/product/${productId}`)
export const getSpecificationById = (id) => request.get(`/specifications/${id}`)
export const addSpecification = (data) => request.post('/specifications', data)
export const updateSpecification = (id, data) => request.put(`/specifications/${id}`, data)
export const deleteSpecification = (id) => request.delete(`/specifications/${id}`)

// 订单相关
export const createOrder = (data) => request.post('/orders', data)
export const getOrderList = (params) => request.get('/orders', { params })
export const getOrderById = (id) => request.get(`/orders/${id}`)
export const getOrderByNumber = (orderNumber) => request.get(`/orders/number/${orderNumber}`)
export const deleteOrder = (id) => request.delete(`/orders/${id}`)

// 统计相关
export const getRevenueStatistics = (params) => request.get('/statistics/revenue', { params })
export const getDayTotalRevenue = (params) => request.get('/statistics/revenue/day', { params })
export const getHotProducts = (params) => request.get('/statistics/hot-products', { params })

// 文件上传
export const uploadImage = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}
