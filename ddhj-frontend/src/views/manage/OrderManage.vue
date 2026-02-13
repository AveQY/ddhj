<template>
  <div class="order-manage">
    <van-nav-bar title="订单管理" />

    <van-tabs v-model:active="activeTab" @click-tab="onTabChange">
      <van-tab title="全部" name="all" />
      <van-tab title="日期" name="date">
        <van-cell title="选择日期" :value="selectedDate" is-link @click="showDatePicker = true" />
      </van-tab>
    </van-tabs>

    <van-popup v-model:show="showDatePicker" position="bottom">
      <van-date-picker
        v-model="currentDate"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>

    <div class="order-list">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadOrders"
      >
        <div v-for="order in orders" :key="order.id" class="order-card" @click="viewOrder(order)">
          <div class="order-header">
            <span class="order-number">NO. {{ order.orderNumber }}</span>
            <span class="order-date">{{ formatDateTime(order.orderDate) }}</span>
          </div>
          
          <div class="order-items-preview">
            <div v-for="(items, productId) in parseOrderItems(order.items)" :key="productId">
              <div v-for="(item, index) in items" :key="index" class="preview-item">
                <img :src="productImages[productId] || '/placeholder.png'" class="preview-image" @error="handleImageError" />
                <div class="preview-info">
                  <div class="name-row">
                    <span class="preview-name">{{ item['商品名称'] }}</span>
                    <span class="category-tag" v-if="getCategoryName(productId)">{{ getCategoryName(productId) }}</span>
                  </div>
                  <div class="preview-spec" v-if="item['规格内容'] || item['规格id']">
                    规格: {{ getSpecName(productId, item) }}
                  </div>
                </div>
                <div class="preview-count">
                  <div>¥{{ item['商品单价'] }}</div>
                  <div class="qty">x{{ item['购买数量'] }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="order-footer">
            <div class="notes" v-if="order.notes">备注: {{ order.notes }}</div>
            <div class="total">
              共 {{ getTotalCount(order) }} 件, 
              实付 <span class="price">¥{{ order.paidAmount }}</span>
            </div>
          </div>
        </div>
      </van-list>
    </div>

    <!-- 订单详情弹窗 -->
    <van-popup v-model:show="showDetail" position="bottom" round :style="{ height: '80%' }">
      <div class="order-detail" v-if="selectedOrder">
        <div class="detail-header">订单详情</div>
        
        <div class="detail-section">
          <div class="detail-row">
            <span class="label">订单号:</span>
            <span>{{ selectedOrder.orderNumber }}</span>
          </div>
          <div class="detail-row">
            <span class="label">下单时间:</span>
            <span>{{ formatDateTime(selectedOrder.orderDate) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">总金额:</span>
            <span class="amount">¥{{ selectedOrder.totalAmount }}</span>
          </div>
          <div class="detail-row">
            <span class="label">实付金额:</span>
            <span class="amount paid">¥{{ selectedOrder.paidAmount }}</span>
          </div>
          <div class="detail-row" v-if="selectedOrder.notes">
            <span class="label">备注:</span>
            <span>{{ selectedOrder.notes }}</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">商品清单</div>
          <div v-for="(items, productId) in parseOrderItems(selectedOrder.items)" :key="productId">
            <div 
              v-for="(item, index) in items" 
              :key="index" 
              class="item-row clickable-item"
              @click.stop="navToDetail(productId)"
            >
              <img :src="productImages[productId] || '/placeholder.png'" class="item-image" @error="handleImageError" />
              <div class="item-info">
                <div class="name-row">
                  <span class="item-name">{{ item['商品名称'] }}</span>
                  <span class="category-tag" v-if="getCategoryName(productId)">{{ getCategoryName(productId) }}</span>
                </div>
                <div class="item-spec" v-if="item['规格内容'] || item['规格id']">
                  规格: {{ getSpecName(productId, item) }}
                </div>
              </div>
              <div class="item-detail">
                <div>¥{{ item['商品单价'] }}</div>
                <div class="quantity">x{{ item['购买数量'] }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <van-button type="danger" block round @click="onDeleteOrder(selectedOrder.id)">删除订单</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, getProductById, deleteOrder, getSpecificationsByProductId, getAllCategories } from '@/api'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()

const orders = ref([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)

const showDetail = ref(false)
const selectedOrder = ref(null)
const productImages = reactive({})
const productSpecs = reactive({})
const productCategories = reactive({})
const categories = ref([])

const activeTab = ref('date')
const selectedDate = ref('')
const currentDate = ref([])
const showDatePicker = ref(false)

onMounted(() => {
  const today = new Date()
  selectedDate.value = formatDate(today)
  currentDate.value = [
    String(today.getFullYear()),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0')
  ]
  loadCategories()
  refreshList() // 确保按日期初始加载
})

const loadCategories = async () => {
  try {
    categories.value = await getAllCategories()
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const getCategoryName = (productId) => {
  const catId = productCategories[productId]
  if (!catId) return ''
  const category = categories.value.find(c => String(c.id) === String(catId))
  return category ? category.name : ''
}

const loadOrders = async () => {
  if (loading.value && pageNum.value > 1) return
  loading.value = true
  
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: 10
    }

    if (activeTab.value === 'date' && selectedDate.value) {
      params.startDate = `${selectedDate.value} 00:00:00`
      params.endDate = `${selectedDate.value} 23:59:59`
    }

    const res = await getOrderList(params)
    const newRecords = res.records || []
    
    orders.value = [...orders.value, ...newRecords]
    newRecords.forEach(order => fetchProductData(order))
    
    if (newRecords.length < 10) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (error) {
    console.error('加载订单失败', error)
    finished.value = true
  } finally {
    loading.value = false
  }
}

const onTabChange = () => {
  refreshList()
}

const onDateConfirm = ({ selectedValues }) => {
  selectedDate.value = selectedValues.join('-')
  currentDate.value = selectedValues
  showDatePicker.value = false
  refreshList()
}

const refreshList = () => {
  pageNum.value = 1
  orders.value = []
  finished.value = false
  loadOrders()
}

const onDeleteOrder = (id) => {
  showConfirmDialog({
    title: '确认删除',
    message: '确定要删除这一条订单吗？删除后不可恢复。'
  }).then(async () => {
    try {
      await deleteOrder(id)
      showToast('删除成功')
      showDetail.value = false
      refreshList()
    } catch (error) {
      console.error('删除订单失败', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

const viewOrder = async (order) => {
  selectedOrder.value = order
  showDetail.value = true
  await fetchProductData(order)
}

const navToDetail = (productId) => {
  router.push(`/goods/${productId}`)
}

const fetchProductData = async (order) => {
  if (!order || !order.items) return
  
  const productIds = Object.keys(order.items).filter(key => key !== 'notes')
  
  for (const id of productIds) {
    if (!productImages[id] || !productSpecs[id] || !productCategories[id]) {
      try {
        const [product, specs] = await Promise.all([
          getProductById(id),
          getSpecificationsByProductId(id)
        ])
        
        if (product) {
          if (product.images && product.images.length > 0) {
            productImages[id] = product.images[0]
          }
          productCategories[id] = product.categoryId
        }
        if (specs) {
          productSpecs[id] = specs
        }
      } catch (error) {
        console.error(`Failed to fetch product data for ${id}:`, error)
      }
    }
  }
}

const getSpecName = (productId, item) => {
  // 1. 优先使用订单中保存的名称
  if (item['规格内容']) return item['规格内容']
  
  // 2. 否则通过规格ID在缓存中查找
  const specId = item['规格id']
  if (!specId) return null
  
  const specs = productSpecs[productId]
  if (specs && Array.isArray(specs)) {
    const spec = specs.find(s => String(s.id) === String(specId))
    if (spec) return spec.name
  }
  
  // 3. 最后退回到显示ID
  return specId
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatDateTime = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const parseOrderItems = (items) => {
  if (!items) return {}
  const result = {}
  for (const [key, value] of Object.entries(items)) {
    if (key !== 'notes') {
      result[key] = value
    }
  }
  return result
}

const getTotalCount = (order) => {
  if (!order || !order.items) return 0
  let count = 0
  const parsedItems = parseOrderItems(order.items)
  Object.values(parsedItems).forEach(items => {
    if (Array.isArray(items)) {
      items.forEach(item => {
        count += Number(item['购买数量'] || 0)
      })
    }
  })
  return count
}
const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E暂无图片%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
.order-manage {
  min-height: 100vh;
  background: #f5f5f5;
}

.order-list {
  padding: 12px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 10px;
  font-size: 13px;
}

.order-number {
  font-weight: 500;
  color: #333;
}

.order-date {
  color: #999;
}

.order-items-preview {
  margin-bottom: 10px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.preview-image {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
  background: #f9f9f9;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-spec {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.preview-count {
  text-align: right;
  font-size: 13px;
  color: #333;
}

.preview-count .qty {
  color: #999;
  font-size: 12px;
  margin-top: 2px;
}

.order-footer {
  text-align: right;
  font-size: 13px;
  color: #666;
  border-top: 1px solid #f5f5f5;
  padding-top: 10px;
}

.notes {
  text-align: left;
  background: #fdf6ec;
  color: #e6a23c;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}

.order-footer .total {
  color: #333;
}

.order-footer .price {
  font-size: 16px;
  font-weight: bold;
  color: #ff4444;
}

.detail-actions {
  margin-top: 30px;
  padding: 0 10px 20px;
}

.order-detail {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.detail-header {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  text-align: center;
}

.detail-section {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-row .label {
  color: #666;
}

.detail-row .amount {
  color: #333;
  font-weight: bold;
}

.detail-row .amount.paid {
  color: #ff4444;
}

.item-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.clickable-item {
  cursor: pointer;
  transition: opacity 0.2s;
}

.clickable-item:active {
  opacity: 0.7;
}

.item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.item-image {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}

.item-row:last-child {
  border-bottom: none;
}

.item-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.category-tag {
  font-size: 11px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.item-spec {
  font-size: 12px;
  color: #999;
  margin-top: 3px;
}

.item-detail {
  text-align: right;
}

.quantity {
  color: #999;
  font-size: 12px;
  margin-top: 3px;
}
</style>
