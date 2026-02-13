<template>
  <div class="dashboard">
    <van-nav-bar title="数据统计">
      <template #right>
        <van-icon name="replay" :class="{ 'refresh-loading': refreshing }" @click="refreshData" />
      </template>
    </van-nav-bar>
    
    <div class="dashboard-content">
      <!-- 收入与图表结合 -->
      <div class="combined-card">
        <div class="revenue-overlay">
          <div class="label">{{ isToday ? '今日总收入' : '当日总收入' }}</div>
          <div class="amount">¥{{ todayRevenue.toFixed(2) }}</div>
        </div>
        
        <div class="chart-controls">
          <div class="date-trigger" @click="showDatePicker = true">
            <van-icon name="calendar-o" /> {{ selectedDate }}
          </div>
          <div class="mode-tabs">
            <span v-for="mode in ['hour', 'day', 'month']" :key="mode" 
                  :class="{ active: chartMode === mode }" @click="changeChartMode(mode)">
              {{ mode === 'hour' ? '时' : mode === 'day' ? '日' : '月' }}
            </span>
          </div>
        </div>

        <div ref="chartRef" class="chart"></div>
      </div>

      <!-- 热销榜单 -->
      <div class="hot-products-container">
        <div class="hot-header">
          <div class="title" @click="showRangePicker = true">
            {{ rangeLabel }} <van-icon name="arrow-down" />
          </div>
          <van-tabs v-model:active="activeHotCategory" class="hot-tabs" shrink>
            <van-tab title="全部" name="all" />
            <van-tab v-for="cat in categories" :key="cat.id" :title="cat.name" :name="cat.id" />
          </van-tabs>
        </div>

        <van-popup v-model:show="showRangePicker" position="bottom" round>
          <van-picker
            :columns="rangeColumns"
            @confirm="onRangeConfirm"
            @cancel="showRangePicker = false"
          />
        </van-popup>

        <div v-if="filteredHotProducts.length > 0" class="hot-list">
          <div 
            v-for="(product, index) in filteredHotProducts" 
            :key="product.productId" 
            class="hot-product-item"
            @click="navToDetail(product.productId)"
          >
            <div class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <img :src="product.images && product.images.length > 0 ? product.images[0] : '/placeholder.png'" class="product-img" />
            <div class="product-detail">
              <div class="product-name">{{ product.productName }}</div>
              <div class="product-meta">
                <span class="product-category">{{ getCategoryName(product.categoryId) }}</span>
                <span class="product-price">¥{{ product.sellPrice }}</span>
              </div>
            </div>
            <div class="sales-count">销量: {{ product.sales }}</div>
          </div>
        </div>
        <div v-else class="empty-state">
          <van-empty description="暂未出售商品~" />
        </div>
      </div>

      <van-popup v-model:show="showDatePicker" position="bottom">
        <van-date-picker
          v-model="currentDate"
          @confirm="onDateConfirm"
          @cancel="showDatePicker = false"
        />
      </van-popup>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRevenueStatistics, getDayTotalRevenue, getHotProducts, getAllCategories } from '@/api'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const chartRef = ref(null)
let chartInstance = null

const selectedDate = ref('')
const currentDate = ref(['2026', '02', '06'])
const showDatePicker = ref(false)
const chartMode = ref('hour')
const todayRevenue = ref(0)
const hotProducts = ref([])
const categories = ref([])
const activeHotCategory = ref('all')
const refreshing = ref(false)

const showRangePicker = ref(false)
const rangeType = ref('day') // day, range, month
const rangeValue = ref(1) // for range type: 2-10
const rangeLabel = ref('日榜单')

const rangeColumns = [
  { text: '日榜单', value: 'day' },
  { text: '2日榜单', value: '2' },
  { text: '3日榜单', value: '3' },
  { text: '4日榜单', value: '4' },
  { text: '5日榜单', value: '5' },
  { text: '6日榜单', value: '6' },
  { text: '7日榜单', value: '7' },
  { text: '8日榜单', value: '8' },
  { text: '9日榜单', value: '9' },
  { text: '10日榜单', value: '10' },
  { text: '月榜单', value: 'month' },
]

const isToday = computed(() => {
  return selectedDate.value === formatDate(new Date())
})

const filteredHotProducts = computed(() => {
  if (activeHotCategory.value === 'all') return hotProducts.value
  return hotProducts.value.filter(p => p.categoryId === activeHotCategory.value)
})

onMounted(() => {
  const today = new Date()
  selectedDate.value = formatDate(today)
  currentDate.value = [
    String(today.getFullYear()),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0')
  ]
  loadCategories()
  refreshData()
})

const loadCategories = async () => {
  try {
    categories.value = await getAllCategories()
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const getCategoryName = (id) => {
  const category = categories.value.find(c => c.id === id)
  return category ? category.name : '未知分类'
}

const navToDetail = (productId) => {
  router.push(`/goods/${productId}`)
}

const refreshData = async () => {
  if (refreshing.value) return
  refreshing.value = true
  try {
    await Promise.all([
      loadRevenueData(),
      loadTodayRevenue(),
      loadHotProducts()
    ])
  } finally {
    refreshing.value = false
  }
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const onDateConfirm = ({ selectedValues }) => {
  currentDate.value = selectedValues
  selectedDate.value = selectedValues.join('-')
  showDatePicker.value = false
  refreshData()
}

const onRangeConfirm = ({ selectedOptions }) => {
  const option = selectedOptions[0]
  rangeLabel.value = option.text
  const val = option.value
  
  if (val === 'day') {
    rangeType.value = 'day'
  } else if (val === 'month') {
    rangeType.value = 'month'
  } else {
    rangeType.value = 'range'
    rangeValue.value = parseInt(val)
  }
  
  showRangePicker.value = false
  loadHotProducts()
}

const changeChartMode = (mode) => {
  chartMode.value = mode
  loadRevenueData()
}

const loadRevenueData = async () => {
  try {
    const res = await getRevenueStatistics({
      date: selectedDate.value,
      mode: chartMode.value
    })
    
    await nextTick()
    renderChart(res.data || [])
  } catch (error) {
    console.error('加载收入数据失败', error)
  }
}

const loadTodayRevenue = async () => {
  try {
    todayRevenue.value = await getDayTotalRevenue({ date: selectedDate.value })
  } catch (error) {
    console.error('加载当日收入失败', error)
  }
}

const loadHotProducts = async () => {
  try {
    let startDate, endDate
    const baseDate = new Date(selectedDate.value)
    
    if (rangeType.value === 'day') {
      startDate = selectedDate.value
      endDate = selectedDate.value
    } else if (rangeType.value === 'month') {
      const firstDay = new Date(baseDate.getFullYear(), baseDate.getMonth(), 1)
      const lastDay = new Date(baseDate.getFullYear(), baseDate.getMonth() + 1, 0)
      startDate = formatDate(firstDay)
      endDate = formatDate(lastDay)
    } else {
      // rangeVal days including baseDate
      const start = new Date(baseDate)
      start.setDate(start.getDate() - (rangeValue.value - 1))
      startDate = formatDate(start)
      endDate = selectedDate.value
    }

    hotProducts.value = await getHotProducts({
      startDate,
      endDate,
      limit: 1000 // Show all sold products
    })
  } catch (error) {
    console.error('加载热销商品失败', error)
  }
}

const renderChart = (data) => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.time)
    },
    yAxis: {
      type: 'value',
      name: '收入 (元)'
    },
    series: [{
      data: data.map(item => item.revenue),
      type: 'line',
      smooth: true,
      itemStyle: {
        color: '#1989fa'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: 'rgba(25, 137, 250, 0.3)'
          }, {
            offset: 1,
            color: 'rgba(25, 137, 250, 0.05)'
          }]
        }
      }
    }]
  }

  chartInstance.setOption(option)
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: #f8f9fa;
}

.dashboard-content {
  padding: 12px;
}

.combined-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  position: relative;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  overflow: hidden;
}

.revenue-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
}

.revenue-overlay .label {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.revenue-overlay .amount {
  font-size: 28px;
  font-weight: 800;
  color: #333;
}

.chart-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.date-trigger {
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mode-tabs {
  background: #f5f5f5;
  border-radius: 6px;
  padding: 2px;
  display: flex;
}

.mode-tabs span {
  padding: 3px 12px;
  font-size: 12px;
  color: #999;
  border-radius: 4px;
  transition: all 0.3s;
}

.mode-tabs span.active {
  background: #fff;
  color: #1989fa;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.chart {
  width: 100%;
  height: 280px;
  margin-top: 40px;
}

.hot-products-container {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.hot-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.hot-tabs {
  --van-tabs-nav-background: transparent;
  width: 100%;
}

.hot-header .title {
  font-size: 17px;
  font-weight: bold;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hot-product-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
  padding: 8px;
  margin: -8px;
  border-radius: 12px;
}

.hot-product-item:active {
  background-color: #f5f5f5;
}

.rank {
  width: 24px;
  height: 24px;
  background: #f0f0f0;
  color: #999;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
}

.rank-1 { background: #ff4d4f; color: #fff; }
.rank-2 { background: #ffa940; color: #fff; }
.rank-3 { background: #ffec3d; color: #333; }

.product-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-category {
  font-size: 12px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
}

.product-price {
  color: #ff4444;
  font-size: 13px;
  font-weight: 500;
}

.sales-count {
  font-size: 13px;
  color: #999;
}

.empty-state {
  padding: 40px 0;
}

.refresh-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
