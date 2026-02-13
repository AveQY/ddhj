<template>
  <div class="order-confirm">
    <van-nav-bar title="确认订单" left-arrow @click-left="goBack" />
    
    <div class="order-content">
      <div class="order-number">
        <span>订单号:</span>
        <span class="number">{{ orderNumber }}</span>
      </div>

      <div class="order-items">
        <div class="section-title">商品清单</div>
        <div v-for="(item, index) in cartItems" :key="index" class="order-item">
          <img :src="productImages[item.productId] || item.image || '/placeholder.png'" class="item-image" @error="handleImageError" />
          <div class="item-content">
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-category" v-if="item.categoryName">{{ item.categoryName }}</div>
              <div class="item-spec" v-if="item.specName">规格: {{ item.specName }}</div>
            </div>
            <div class="item-detail">
              <span class="item-price">¥{{ item.price }}</span>
              <span class="item-quantity">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="order-total">
        <span>总金额:</span>
        <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>

      <div class="order-notes">
        <div class="section-title">备注信息</div>
        <van-field
          v-model="notes"
          rows="3"
          autosize
          type="textarea"
          placeholder="请输入备注信息（选填）"
        />
      </div>
    </div>

    <div class="order-footer">
      <van-button 
        type="primary" 
        block 
        @touchstart="handleTouchStart"
        @touchend="handleTouchEnd"
        @mousedown="handleMouseDown"
        @mouseup="handleMouseUp"
        @mouseleave="handleMouseLeave"
      >
        下单
      </van-button>
    </div>

    <!-- 实付金额输入弹窗 -->
    <van-dialog
      v-model:show="showPaymentDialog"
      title="输入实付金额"
      show-cancel-button
      @confirm="submitOrder"
    >
      <div class="payment-input">
        <van-field
          v-model="paidAmount"
          type="number"
          label="实付金额"
          placeholder="请输入实付金额"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createOrder, getProductById, getAllCategories } from '@/api'
import { showToast, showSuccessToast } from 'vant'
import { useCartStore } from '@/utils/cartStore'

const router = useRouter()
const { clearCart } = useCartStore()

const orderNumber = ref('')
const cartItems = ref([])
const notes = ref('')
const paidAmount = ref('')
const showPaymentDialog = ref(false)
const productImages = ref({})
const longPressTimer = ref(null)
const isLongPress = ref(false)

const totalAmount = computed(() => {
  if (!cartItems.value || !Array.isArray(cartItems.value)) return 0
  return cartItems.value.reduce((sum, item) => {
    const price = parseFloat(item.price) || 0
    const quantity = parseInt(item.quantity) || 0
    return sum + price * quantity
  }, 0)
})

onMounted(() => {
  console.log('OrderConfirm mounted, history.state:', history.state)
  
  // 生成临时订单号
  const timestamp = new Date().getTime()
  const random = Math.floor(Math.random() * 1000000).toString().padStart(6, '0')
  orderNumber.value = timestamp + random

  // 获取购物车数据
  try {
    if (history.state && history.state.cartItems) {
      cartItems.value = history.state.cartItems
      console.log('Cart items loaded:', cartItems.value)
    } else {
      console.warn('No cart items in history.state')
      showToast('订单信息丢失，请重新选择商品')
      setTimeout(() => {
        router.push('/')
      }, 1500)
    }
  } catch (err) {
    console.error('Error parsing cart items:', err)
    showToast('发生错误，请返回重试')
    router.back()
  }

  // 动态获取缺失的商品图片和分类信息
  loadCategories()
  fetchMissingImages()

  // 默认实付金额为总金额
  paidAmount.value = totalAmount.value.toFixed(2)
})

const fetchMissingImages = async () => {
  if (!cartItems.value) return
  
  for (const item of cartItems.value) {
    // 检查是否缺失图片或分类名称
    const missingImage = !item.image && !productImages.value[item.productId]
    const missingCategory = !item.categoryName

    if (missingImage || missingCategory) {
      try {
        const product = await getProductById(item.productId)
        if (product) {
          // 补充图片
          if (missingImage && product.images && product.images.length > 0) {
            productImages.value[item.productId] = product.images[0]
          }
          // 补充分类名称，需要额外获取分类列表或者后端接口支持
          // 这里简化处理：如果我们已经有了 item.categoryId 但没有 name，或者完全没有
          // 由于 getProductById 可能不返回 categoryName，我们需要根据 categoryId 再查一次
          // 或者如果后端 getProductById 返回了 categoryId，我们可以尝试通过 getAllCategories 获取
          // 但是这里 getAllCategories 可能代价太大。
          // 更好的方式是：修改 getProductById 接口返回 categoryName，但这需要后端修改。
          // 既然 UserHome 已经加载了 Categories，我们可以假设 categoryName 应该在 UserHome 阶段就有了。
          // 如果这里确实没有，我们可以尝试加载所有 categories（会有缓存）
        }
      } catch (error) {
        console.error(`Failed to fetch product ${item.productId} details:`, error)
      }
    }
  }
}

// 补充：为了在 OrderConfirm 中也能获取分类名称，我们可以在 onMounted 中加载分类列表
// 然后根据 productId 对应的 categoryId (如果 item 里没有，就得从 product 详情拿) 匹配
const categories = ref([])
const loadCategories = async () => {
  try {
    const res = await getAllCategories()
    categories.value = res || []
    
    // 尝试修复缺失的 categoryName
    if (cartItems.value) {
      for (const item of cartItems.value) {
        if (!item.categoryName) {
           // 如果 item 里有 categoryId (通常没有，cartStore 也没存)，我们需要先获取 product 详情
           try {
             const product = await getProductById(item.productId)
             if (product && product.categoryId) {
               const cat = categories.value.find(c => c.id === product.categoryId)
               if (cat) {
                 item.categoryName = cat.name
               }
             }
           } catch (e) {
             console.error('Fix category name failed', e)
           }
        }
      }
    }
  } catch (error) {
    console.error('Load categories failed', error)
  }
}

const goBack = () => {
  router.back()
}

const startTimer = () => {
  isLongPress.value = false
  longPressTimer.value = setTimeout(() => {
    isLongPress.value = true
    showPaymentDialog.value = true
    longPressTimer.value = null
  }, 800)
}

const endTimer = () => {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

const handleAction = () => {
  if (!isLongPress.value) {
    // 点击：直接下单，实付金额为总金额
    paidAmount.value = totalAmount.value.toFixed(2)
    submitOrder()
  }
}

const handleTouchStart = (e) => {
  // e.preventDefault() // 可能会影响某些浏览器的交互，先注释掉
  startTimer()
}

const handleTouchEnd = (e) => {
  // e.preventDefault()
  endTimer()
  handleAction()
}

const handleMouseDown = () => {
  startTimer()
}

const handleMouseUp = () => {
  endTimer()
  handleAction()
}

const handleMouseLeave = () => {
  endTimer()
}

const submitOrder = async () => {
  if (!paidAmount.value || parseFloat(paidAmount.value) <= 0) {
    showToast('请输入有效的实付金额')
    return
  }

  try {
    // 构建订单商品数据
    const items = {}
    cartItems.value.forEach(item => {
      const productId = item.productId.toString()
      if (!items[productId]) {
        items[productId] = []
      }
      items[productId].push({
        '商品名称': item.productName,
        '图片': item.image,
        '规格id': item.specId ? item.specId.toString() : null,
        '规格内容': item.specName || null,
        '购买数量': item.quantity.toString(),
        '商品单价': item.price.toString()
      })
    })
    
    if (notes.value) {
      items['notes'] = notes.value
    }

    const formatDate = (date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }

    const orderData = {
      orderNumber: orderNumber.value,
      items: items,
      totalAmount: Number(totalAmount.value),
      paidAmount: Number(paidAmount.value),
      notes: notes.value,
      orderDate: formatDate(new Date())
    }

    await createOrder(orderData)
    
    // 下单成功，清除购物车
    clearCart()
    
    showSuccessToast('下单成功')
    
    // 返回主页
    setTimeout(() => {
      router.push('/')
    }, 1500)
  } catch (error) {
    console.error('下单失败', error)
    showToast('下单失败，请重试')
  }
}
const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E暂无图片%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
.order-confirm {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 70px;
}

.order-content {
  padding: 15px;
}

.order-number {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-number .number {
  font-weight: bold;
  color: #1989fa;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.order-items {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  max-height: 50vh;
  overflow-y: auto;
}

.order-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}

.item-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 14px;
  margin-bottom: 5px;
}

.item-category {
  font-size: 12px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 4px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 3px;
}

.item-spec {
  font-size: 12px;
  color: #999;
}

.item-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.item-price {
  color: #ff4444;
  font-size: 14px;
}

.item-quantity {
  color: #999;
  font-size: 12px;
}

.order-total {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}

.total-amount {
  color: #ff4444;
  font-size: 20px;
  font-weight: bold;
}

.order-notes {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
}

.order-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 15px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
}

.payment-input {
  padding: 20px;
}
</style>
