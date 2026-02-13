<template>
  <div class="item-detail-page">
    <van-nav-bar title="商品详情" left-arrow @click-left="goBack" />
    
    <div class="content" v-if="product">
      <!-- 横向滑动图片 -->
      <div class="image-scroll-container" v-if="parsedImages.length > 0">
        <div class="scroll-wrapper">
          <img 
            v-for="(img, idx) in parsedImages" 
            :key="idx" 
            :src="img" 
            class="scroll-img" 
            @error="handleImageError" 
          />
        </div>
      </div>
      <div v-else class="image-placeholder">
        <img src="/placeholder.png" class="scroll-img" />
      </div>

      <div class="info-section">
        <div class="price-row">
          <span class="currency">¥</span>
          <span class="price">{{ product.sellPrice }}</span>
        </div>
        <div class="title">{{ product.name }}</div>
        <div class="description" v-if="product.description">{{ product.description }}</div>
      </div>

      <!-- 规格 -->
      <div class="spec-section" v-if="specifications.length > 0">
        <div class="section-title">规格选择</div>
        <div class="spec-list">
          <div 
            v-for="spec in specifications" 
            :key="spec.id" 
            class="spec-tag"
            :class="{ active: selectedSpec && selectedSpec.id === spec.id, disabled: spec.stock === 0 }"
            @click="selectSpec(spec)"
          >
            {{ spec.name }}
          </div>
        </div>
      </div>
    </div>

    <van-action-bar>
      <van-action-bar-icon icon="cart-o" text="购物车" :badge="totalBadgeCount" @click="goToHome" />
      <van-action-bar-button type="warning" text="加入购物车" @click="handleAddToCart" />
      <van-action-bar-button type="danger" text="立即购买" @click="handleBuyNow" />
    </van-action-bar>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductById, getSpecificationsByProductId } from '../api/index'
import { showToast } from 'vant'

const route = useRoute()
const router = useRouter()
const pId = route.params.id

const product = ref(null)
const specifications = ref([])
const selectedSpec = ref(null)
const cartItems = ref([])

onMounted(async () => {
  try {
    const res = await getProductById(pId)
    product.value = res
    
    const specs = await getSpecificationsByProductId(pId)
    specifications.value = specs || []
    
    const saved = localStorage.getItem('ddhj_cart')
    if (saved) {
      cartItems.value = JSON.parse(saved)
    }
  } catch (error) {
    console.error('Fetch error:', error)
  }
})

// 兼容数据库存储逻辑
const parsedImages = computed(() => {
  if (!product.value || !product.value.images) return []
  const imgs = product.value.images
  // 如果是字符串（JSON 数组格式），尝试解析
  if (typeof imgs === 'string' && imgs.startsWith('[')) {
    try {
      return JSON.parse(imgs)
    } catch (e) {
      return [imgs]
    }
  }
  // 已经是数组
  if (Array.isArray(imgs)) return imgs
  // 单个字符串
  if (typeof imgs === 'string' && imgs) return [imgs]
  return []
})

const totalBadgeCount = computed(() => {
  const count = cartItems.value.reduce((s, i) => s + (i.quantity || 0), 0)
  return count > 0 ? count : ''
})

const goBack = () => router.back()
const goToHome = () => router.push('/')

const selectSpec = (s) => {
  if (s.stock === 0) return
  selectedSpec.value = s
}

const handleAddToCart = () => {
  if (!product.value) return
  if (specifications.value.length > 0 && !selectedSpec.value) {
    showToast('请选择规格')
    return
  }
  
  const item = {
    productId: product.value.id,
    productName: product.value.name,
    image: parsedImages.value.length > 0 ? parsedImages.value[0] : null,
    specId: selectedSpec.value ? selectedSpec.value.id : null,
    specName: selectedSpec.value ? selectedSpec.value.name : null,
    price: product.value.sellPrice,
    quantity: 1
  }

  const existing = cartItems.value.find(i => 
    i.productId === item.productId && i.specId === item.specId
  )

  if (existing) {
    existing.quantity += 1
  } else {
    cartItems.value.push(item)
  }
  
  localStorage.setItem('ddhj_cart', JSON.stringify(cartItems.value))
  showToast('加购成功')
}

const handleBuyNow = () => {
  if (!product.value) return
  if (specifications.value.length > 0 && !selectedSpec.value) {
    showToast('请选择规格')
    return
  }
  
  const item = {
    productId: product.value.id,
    productName: product.value.name,
    image: parsedImages.value.length > 0 ? parsedImages.value[0] : null,
    specId: selectedSpec.value ? selectedSpec.value.id : null,
    specName: selectedSpec.value ? selectedSpec.value.name : null,
    price: product.value.sellPrice,
    quantity: 1
  }
  
  router.push({
    name: 'OrderConfirm',
    state: { cartItems: [item] }
  })
}

const handleImageError = (e) => {
  e.target.src = '/placeholder.png'
}
</script>

<style scoped>
.item-detail-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 60px;
}
.image-scroll-container {
  width: 100%;
  height: 375px;
  background: #fff;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.scroll-wrapper {
  display: flex;
  height: 100%;
}
.scroll-img {
  flex-shrink: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-placeholder {
  height: 375px;
  background: #fff;
}
.image-scroll-container::-webkit-scrollbar {
  display: none;
}
.info-section {
  background: #fff;
  padding: 16px;
  margin-bottom: 12px;
}
.price-row {
  color: #ee0a24;
  margin-bottom: 8px;
}
.currency { font-size: 14px; }
.price { font-size: 24px; font-weight: bold; }
.title { font-size: 18px; font-weight: bold; color: #333; }
.description { font-size: 14px; color: #666; margin-top: 8px; line-height: 1.6; }
.spec-section { background: #fff; padding: 16px; }
.section-title { font-size: 14px; font-weight: bold; margin-bottom: 12px; }
.spec-list { display: flex; flex-wrap: wrap; gap: 10px; }
.spec-tag {
  padding: 6px 16px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
}
.spec-tag.active {
  background: #fff1f0;
  color: #ee0a24;
  border: 1px solid #ee0a24;
}
.spec-tag.disabled { color: #ccc; }
</style>
