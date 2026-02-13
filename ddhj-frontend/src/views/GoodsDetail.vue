<template>
  <div class="goods-detail">
    <van-nav-bar fixed placeholder title="商品详情" left-arrow @click-left="goBack" />
    
    <div v-if="product" class="detail-content">
      <!-- 轮播图 -->
      <van-swipe class="my-swipe" :autoplay="3000" indicator-color="white">
        <van-swipe-item v-for="(img, index) in images" :key="index">
          <img :src="img" class="swipe-img" @click="previewImage(index)" @error="handleImageError" />
        </van-swipe-item>
        <template #indicator="{ active, total }">
          <div class="custom-indicator">{{ active + 1 }}/{{ total }}</div>
        </template>
      </van-swipe>

      <div class="goods-info">
        <div class="price-section">
          <span class="currency">¥</span>
          <span class="price">{{ product.sellPrice }}</span>
        </div>
        <div class="title">{{ product.name }}</div>
        <div class="description">{{ product.description }}</div>
      </div>

      <!-- 规格选择 -->
      <div class="spec-cell" @click="showSpecPopup = true">
        <div class="cell-label">选择</div>
        <div class="cell-value">{{ selectedSpec ? selectedSpec.name : '请选择规格' }}</div>
        <van-icon name="arrow" />
      </div>

      <!-- 商品分类 -->
      <div v-if="categoryName" class="spec-cell info-cell">
        <div class="cell-label">分类</div>
        <div class="cell-value">{{ categoryName }}</div>
      </div>

      <!-- 商品展示 -->
      <div class="goods-desc">
        <div class="section-title">商品介绍</div>
        <div class="desc-content">
          <template v-if="images.length > 0">
            <img v-for="(img, idx) in images" :key="idx" :src="img" class="desc-img" />
          </template>
          <div v-else class="no-desc">暂无详细介绍</div>
        </div>
      </div>
    </div>

    <div v-else class="loading-state">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <!-- 规格弹窗 -->
    <van-popup v-model:show="showSpecPopup" position="bottom" round closeable>
      <div class="spec-popup">
        <div class="spec-header">
          <img :src="specImage" class="spec-img" @error="handleImageError" />
          <div class="spec-info">
            <div class="spec-price">¥{{ product?.sellPrice }}</div>
            <div class="spec-selected">已选: {{ selectedSpec ? selectedSpec.name : '未选择' }}</div>
            <div class="spec-stock" v-if="selectedSpec">库存: {{ selectedSpec.stock }}</div>
          </div>
        </div>
        
        <div class="spec-body">
          <div class="spec-title">规格</div>
          <div class="spec-list">
            <div 
              v-for="spec in specifications" 
              :key="spec.id"
              class="spec-item"
              :class="{ active: selectedSpec?.id === spec.id, disabled: spec.stock === 0 }"
              @click="selectSpec(spec)"
            >
              {{ spec.name }} <span class="item-stock" v-if="spec.stock <= 10 && spec.stock > 0">({{ spec.stock }})</span>
            </div>
          </div>

          <div class="quantity-section">
            <div class="spec-title">数量</div>
            <van-stepper v-model="quantity" :min="1" :max="selectedSpec ? selectedSpec.stock : 99" />
          </div>
        </div>

        <div class="spec-footer">
          <van-button type="primary" block round @click="confirmSpec">确定</van-button>
        </div>
      </div>
    </van-popup>

    <van-action-bar>
      <van-action-bar-icon icon="chat-o" text="客服" />
      <van-action-bar-icon icon="cart-o" text="购物车" :badge="cartBadge" @click="goToCart" />
      <van-action-bar-button type="warning" text="加入购物车" @click="addToCart" />
      <van-action-bar-button type="danger" text="立即购买" @click="buyNow" />
    </van-action-bar>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductById, getSpecificationsByProductId, getAllCategories } from '@/api'
import { showToast, showImagePreview } from 'vant'
import { useCartStore } from '@/utils/cartStore'

const route = useRoute()
const router = useRouter()
const { totalCount: cartCount, addToCart: storeAddToCart } = useCartStore()

const product = ref(null)
const specifications = ref([])
const categories = ref([])
const selectedSpec = ref(null)
const quantity = ref(1)
const showSpecPopup = ref(false)
const pendingAction = ref(null) // 'add' or 'buy'

onMounted(async () => {
  const productId = route.params.id
  try {
    const [productData, specData, categoryData] = await Promise.all([
      getProductById(productId),
      getSpecificationsByProductId(productId),
      getAllCategories()
    ])
    product.value = productData
    specifications.value = specData || []
    categories.value = categoryData || []
  } catch (e) {
    console.error('Failed to load product detail', e)
    showToast('加载失败')
  }
})

const categoryName = computed(() => {
  if (!product.value || !product.value.categoryId) return ''
  const category = categories.value.find(c => c.id === product.value.categoryId)
  return category ? category.name : ''
})

const images = computed(() => {
  if (!product.value || !product.value.images) return []
  const imgs = product.value.images
  if (Array.isArray(imgs)) return imgs
  if (typeof imgs === 'string' && imgs.startsWith('[')) {
    try { return JSON.parse(imgs) } catch (e) { return [imgs] }
  }
  if (typeof imgs === 'string' && imgs) return [imgs]
  return []
})

const specImage = computed(() => {
  return images.value[0] || '/placeholder.png'
})

const cartBadge = computed(() => {
  return cartCount.value > 0 ? cartCount.value : ''
})

const goBack = () => router.back()

const goToCart = () => {
  router.push('/')
}

const selectSpec = (spec) => {
  if (spec.stock === 0) return
  selectedSpec.value = spec
}

const previewImage = (index) => {
  showImagePreview({
    images: images.value,
    startPosition: index,
  })
}

const confirmSpec = () => {
  if (specifications.value.length > 0 && !selectedSpec.value) {
    showToast('请选择规格')
    return
  }
  showSpecPopup.value = false
  
  if (pendingAction.value === 'add') {
    executeAddToCart()
  } else if (pendingAction.value === 'buy') {
    executeBuyNow()
  }
  pendingAction.value = null
}

const addToCart = () => {
  if (specifications.value.length > 0 && !selectedSpec.value) {
    pendingAction.value = 'add'
    showSpecPopup.value = true
    return
  }
  executeAddToCart()
}

const executeAddToCart = () => {
  storeAddToCart(product.value, selectedSpec.value, quantity.value)
  showToast('已加入购物车')
  
  // 返回主页
  setTimeout(() => {
    router.push('/')
  }, 1000)
}

const buyNow = () => {
  if (specifications.value.length > 0 && !selectedSpec.value) {
    pendingAction.value = 'buy'
    showSpecPopup.value = true
    return
  }
  executeBuyNow()
}

const executeBuyNow = () => {
  const item = {
    productId: product.value.id,
    productName: product.value.name,
    image: images.value[0] || null,
    specId: selectedSpec.value ? selectedSpec.value.id : null,
    specName: selectedSpec.value ? selectedSpec.value.name : null,
    price: product.value.sellPrice,
    quantity: quantity.value
  }

  router.push({
    name: 'OrderConfirm',
    state: { cartItems: [item] }
  })
}

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E暂无图片%3C/text%3E%3C/svg%3E'
}
</script>

<style scoped>
.goods-detail {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.my-swipe {
  height: 375px;
  background: #fff;
}

.swipe-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.custom-indicator {
  position: absolute;
  right: 15px;
  bottom: 15px;
  padding: 2px 8px;
  color: #fff;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
}

.goods-info {
  background: #fff;
  padding: 15px;
  margin-bottom: 10px;
}

.price-section {
  color: #ee0a24;
  margin-bottom: 8px;
}

.price-section .currency {
  font-size: 14px;
}

.price-section .price {
  font-size: 24px;
  font-weight: bold;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.description {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.spec-cell {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 15px;
  margin-bottom: 10px;
  cursor: pointer;
}

.cell-label {
  color: #999;
  width: 40px;
  font-size: 14px;
}

.cell-value {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.goods-desc {
  background: #fff;
  padding: 15px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 3px solid #ee0a24;
}

.desc-img {
  width: 100%;
  display: block;
}

.no-desc {
  text-align: center;
  padding: 30px;
  color: #999;
  font-size: 14px;
}

.loading-state {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 规格弹窗样式 */
.spec-popup {
  padding: 20px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.spec-header {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.spec-img {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
}

.spec-price {
  color: #ee0a24;
  font-size: 20px;
  font-weight: bold;
  margin-top: 20px;
}

.spec-selected {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.spec-stock {
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

.spec-body {
  flex: 1;
  overflow-y: auto;
}

.spec-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.spec-item {
  padding: 6px 16px;
  background: #f7f8fa;
  border-radius: 4px;
  font-size: 13px;
  border: 1px solid transparent;
}

.spec-item.active {
  background: #fff1f0;
  color: #ee0a24;
  border-color: #ee0a24;
}

.spec-item.disabled {
  color: #c8c9cc;
}

.item-stock {
  font-size: 11px;
  color: #ff4444;
  margin-left: 2px;
}

.quantity-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
}

.spec-footer {
  margin-top: 20px;
}
</style>
