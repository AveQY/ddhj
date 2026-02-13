<template>
  <div class="user-home">
    <!-- 左侧分类栏 -->
    <div class="category-sidebar">
      <div 
        class="category-item" 
        :class="{ active: selectedCategory === null }"
        @click="selectCategory(null)"
      >
        全部
      </div>
      <div 
        v-for="category in categories" 
        :key="category.id"
        class="category-item"
        :class="{ active: selectedCategory === category.id }"
        @click="selectCategory(category.id)"
      >
        {{ category.name }}
      </div>
    </div>

    <!-- 右侧商品列表 -->
    <div class="product-list">
      <div v-if="products.length === 0" class="empty-state">
        暂未找到商品~
      </div>
      <div 
        v-else
        v-for="product in products" 
        :key="product.id"
        class="product-card"
        @click="navToDetail(product.id)"
      >
        <img 
          :src="product.images && product.images.length > 0 ? product.images[0] : '/placeholder.png'" 
          class="product-image"
          @error="handleImageError"
        />
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-category-tag" v-if="getCategoryName(product.categoryId)">
            {{ getCategoryName(product.categoryId) }}
          </div>
          <div class="product-price">¥{{ product.sellPrice }}</div>
        </div>
        <div class="add-btn" @click.stop="addToCart(product, $event)">
          <van-icon name="plus" />
        </div>
      </div>
    </div>

    <!-- 底部购物栏 -->
    <div v-if="cartItems.length > 0" class="cart-bar" @click="showCart = true">
      <div class="cart-info" ref="cartIconRef">
        <van-icon name="shopping-cart-o" size="24" />
        <span class="cart-count">{{ totalCount }}</span>
        <span class="cart-total">¥{{ totalPrice.toFixed(2) }}</span>
      </div>
      <div class="checkout-btn">去下单</div>
    </div>

    <!-- 规格选择弹窗 -->
    <van-popup v-model:show="showSpecSelector" position="bottom" round>
      <div class="spec-selector" v-if="selectedProduct">
        <div class="spec-header">
          <img :src="selectedProduct.images && selectedProduct.images.length > 0 ? selectedProduct.images[0] : '/placeholder.png'" class="spec-image" />
          <div class="spec-info">
            <div class="spec-name">{{ selectedProduct.name }}</div>
            <div class="spec-price">¥{{ selectedProduct.sellPrice }}</div>
            <div class="spec-stock" v-if="selectedSpec">库存: {{ selectedSpec.stock }}</div>
          </div>
        </div>
        
        <div class="spec-options" v-if="specifications.length > 0">
          <div class="spec-title">选择规格</div>
          <div class="spec-buttons">
            <div 
              v-for="spec in specifications" 
              :key="spec.id"
              class="spec-btn"
              :class="{ 
                active: selectedSpec && selectedSpec.id === spec.id,
                disabled: spec.stock === 0
              }"
              @click="selectSpec(spec)"
            >
              {{ spec.name }}
            </div>
          </div>
        </div>

        <div class="spec-quantity">
          <span>数量</span>
          <van-stepper v-model="quantity" :min="1" :max="selectedSpec ? selectedSpec.stock : 999" />
        </div>

        <div class="spec-confirm">
          <van-button type="primary" block @click="confirmAddToCart">确定</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 购物车弹窗 -->
    <van-popup v-model:show="showCart" position="bottom" round>
      <div class="cart-drawer">
        <div class="cart-header">
          <div class="header-left">
            <span>购物车</span>
            <span class="clear-btn" @click="clearCart">清空</span>
          </div>
          <van-icon name="cross" @click="showCart = false" />
        </div>
        
        <div class="cart-items">
          <div v-for="item in cartItems" :key="item.productId + '-' + (item.specId || 'none')" class="cart-item">
            <img :src="item.image || '/placeholder.png'" class="cart-item-image" @error="handleImageError" />
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.productName }}</div>
              <div class="cart-item-category" v-if="item.categoryName">{{ item.categoryName }}</div>
              <div class="cart-item-spec" v-if="item.specName">{{ item.specName }}</div>
              <div class="cart-item-price">¥{{ item.price }}</div>
            </div>
            <van-stepper v-model="item.quantity" :min="0" @change="updateCartItem(item)" />
          </div>
        </div>

        <div class="cart-footer">
          <div class="cart-total-price">
            <span>合计:</span>
            <span class="price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <van-button type="primary" @click="goToCheckout">下单</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 飞入小球容器 -->
    <div class="ball-container">
      <div 
        v-for="ball in balls" 
        :key="ball.id"
        :id="'ball-' + ball.id"
        class="ball"
        :style="{ left: ball.x + 'px', top: ball.y + 'px', display: ball.show ? 'block' : 'none' }"
      >
        <div class="inner"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAllCategories, getProductList, getSpecificationsByProductId } from '@/api'
import { showToast, showConfirmDialog } from 'vant'
import { useCartStore } from '@/utils/cartStore'

const router = useRouter()
const { cartItems, totalCount, totalPrice, addToCart: storeAddToCart, updateQuantity, clearCart: storeClearCart } = useCartStore()

const categories = ref([])
const products = ref([])
const selectedCategory = ref(null)

const showSpecSelector = ref(false)
const selectedProduct = ref(null)
const specifications = ref([])
const selectedSpec = ref(null)
const quantity = ref(1)

const showCart = ref(false)
const cartIconRef = ref(null)
const balls = ref([])
let ballId = 0

onMounted(() => {
  loadCategories()
  loadProducts()
})

const loadCategories = async () => {
  try {
    categories.value = await getAllCategories()
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const loadProducts = async () => {
  try {
    const res = await getProductList({ 
      categoryId: selectedCategory.value,
      pageNum: 1,
      pageSize: 100
    })
    products.value = res.records || []
  } catch (error) {
    console.error('加载商品失败', error)
  }
}

const createFlyingBall = (event) => {
  const { clientX, clientY } = event
  const newBall = {
    id: ballId++,
    x: clientX,
    y: clientY,
    show: true
  }
  balls.value.push(newBall)
  
  nextTick(() => {
    setTimeout(() => {
      const cartPos = cartIconRef.value ? cartIconRef.value.getBoundingClientRect() : { left: 40, top: window.innerHeight - 30 }
      const ballEl = document.getElementById('ball-' + newBall.id)
      if (!ballEl) return
      
      const innerEl = ballEl.querySelector('.inner')
      
      // 关键帧动画通过修改 style 属性触发 CSS transition
      ballEl.style.transform = `translate3d(0, ${cartPos.top - clientY}px, 0)`
      innerEl.style.transform = `translate3d(${cartPos.left - clientX}px, 0, 0)`
      innerEl.style.opacity = '0'
      
      setTimeout(() => {
        balls.value = balls.value.filter(b => b.id !== newBall.id)
      }, 600)
    }, 20)
  })
}

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  loadProducts()
}

const addToCart = async (product, event) => {
  if (event) createFlyingBall(event)
  selectedProduct.value = product
  
  try {
    const specs = await getSpecificationsByProductId(product.id)
    specifications.value = specs || []
    
    if (specifications.value.length > 0) {
      // 有规格，显示规格选择器
      selectedSpec.value = null
      quantity.value = 1
      showSpecSelector.value = true
    } else {
      // 无规格，直接添加
      const category = categories.value.find(c => c.id === product.categoryId)
      const categoryName = category ? category.name : ''
      storeAddToCart(product, null, 1, categoryName)
      showToast('已添加到购物车')
    }
  } catch (error) {
    console.error('获取规格失败', error)
  }
}

const selectSpec = (spec) => {
  if (spec.stock === 0) {
    showToast('该规格已售罄')
    return
  }
  selectedSpec.value = spec
}

const confirmAddToCart = (event) => {
  if (event) createFlyingBall(event)
  const category = categories.value.find(c => c.id === selectedProduct.value.categoryId)
  const categoryName = category ? category.name : ''
  storeAddToCart(selectedProduct.value, selectedSpec.value, quantity.value, categoryName)
  showToast('已添加到购物车')
  showSpecSelector.value = false
}

const clearCart = () => {
  if (cartItems.value.length === 0) return
  showConfirmDialog({
    title: '确认清空',
    message: '确定要清空购物车吗？'
  }).then(() => {
    storeClearCart()
    showToast('已清空')
  }).catch(() => {})
}

const navToDetail = (id) => {
  router.push(`/goods/${id}`)
}

const updateCartItem = (item) => {
  updateQuantity(item, item.quantity)
}

const goToCheckout = () => {
  if (cartItems.value.length === 0) {
    showToast('购物车为空')
    return
  }
  // 使用序列化和反序列化确保传递的是纯对象而非响应式代理
  const items = JSON.parse(JSON.stringify(cartItems.value))
  router.push({
    name: 'OrderConfirm',
    state: { cartItems: items }
  })
}

const getCategoryName = (categoryId) => {
  const category = categories.value.find(c => c.id === categoryId)
  return category ? category.name : ''
}

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E暂无图片%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
.user-home {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.category-sidebar {
  width: 80px;
  flex-shrink: 0;
  background: #fff;
  overflow-y: auto;
  padding-bottom: 70px; /* 为底部购物车栏留出空间 */
  border-right: 1px solid #eee;
}

.category-item {
  padding: 15px 10px;
  text-align: center;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
}

.category-item.active {
  background: #f0f0f0;
  color: #1989fa;
  font-weight: bold;
}

.product-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
  padding-bottom: 70px; /* 为底部购物车栏留出空间 */
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  align-content: start;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  display: flex;
  height: 100px;
}

.product-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  padding: 10px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 14px;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  /* white-space: nowrap;  Remove nowrap to allow wrapping if desired, or keep it */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-category-tag {
  font-size: 12px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
  align-self: flex-start;
  margin-bottom: 5px;
}

.product-price {
  color: #ff4444;
  font-size: 16px;
  font-weight: bold;
}

.add-btn {
  position: absolute;
  bottom: 0px;
  right: 0px;
  width: 30px;
  height: 30px;
  background: #1989fa;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
  cursor: pointer;
}

.cart-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cart-count {
  background: #ff4444;
  color: #fff;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
}

.cart-total {
  color: #ff4444;
  font-size: 18px;
  font-weight: bold;
}

.checkout-btn {
  background: #1989fa;
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
}

.spec-selector {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.spec-header {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.spec-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.spec-info {
  flex: 1;
}

.spec-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.spec-price {
  color: #ff4444;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.spec-stock {
  color: #999;
  font-size: 14px;
}

.spec-options {
  margin-bottom: 20px;
}

.spec-title {
  font-size: 14px;
  margin-bottom: 10px;
  color: #666;
}

.spec-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-btn {
  padding: 8px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.spec-btn.active {
  border-color: #1989fa;
  color: #1989fa;
  background: #e6f7ff;
}

.spec-btn.disabled {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.spec-quantity {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.spec-confirm {
  margin-top: 20px;
}

.cart-drawer {
  padding: 20px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: bold;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clear-btn {
  font-size: 12px;
  font-weight: normal;
  color: #999;
  cursor: pointer;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 12px;
}

.cart-items {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 15px;
}

.cart-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.cart-item-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}

.cart-item-info {
  flex: 1;
}

.cart-item-name {
  font-size: 14px;
  margin-bottom: 3px;
}

.cart-item-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 3px;
}

.cart-item-price {
  color: #ff4444;
  font-size: 14px;
}

.cart-item-category {
  font-size: 12px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 4px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 3px;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.cart-total-price {
  font-size: 14px;
}

.cart-total-price .price {
  color: #ff4444;
  font-size: 18px;
  font-weight: bold;
  margin-left: 10px;
}

/* 飞入小球样式 */
.ball-container .ball {
  position: fixed;
  z-index: 2000;
  transition: all 0.6s cubic-bezier(0.49, -0.29, 0.75, 0.41);
}

.ball-container .ball .inner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #1989fa;
  transition: all 0.6s linear;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
  width: 100%;
}
</style>
