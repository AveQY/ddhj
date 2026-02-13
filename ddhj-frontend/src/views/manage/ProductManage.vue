<template>
  <div class="product-manage">
    <van-nav-bar title="商品管理">
      <template #right>
        <van-button type="primary" size="small" @click="showAddDialog">添加</van-button>
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeCategory" @click-tab="onTabChange" sticky offset-top="46">
      <van-tab title="全部" :name="null" />
      <van-tab v-for="cat in categories" :key="cat.id" :title="cat.name" :name="cat.id" />
    </van-tabs>

    <div class="product-list">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadProducts"
      >
        <div v-for="product in products" :key="product.id" class="product-item">
          <img :src="product.images && product.images.length > 0 ? product.images[0] : '/placeholder.png'" class="product-image" />
          <div class="product-info">
            <div class="product-name-row">
              <div class="product-name">{{ product.name }}</div>
              <span class="product-category" v-if="getCategoryNameById(product.categoryId)">{{ getCategoryNameById(product.categoryId) }}</span>
            </div>
            <div class="price-row">
              <span class="label">进货价:</span>
              <span class="price">¥{{ product.purchasePrice || 0 }}</span>
            </div>
            <div class="price-row">
              <span class="label">出售价:</span>
              <span class="price sell">¥{{ product.sellPrice }}</span>
            </div>
          </div>
          <div class="product-actions">
            <van-button size="small" @click="editProduct(product)">编辑</van-button>
            <van-button size="small" type="danger" @click="deleteProductItem(product.id)">删除</van-button>
          </div>
        </div>
      </van-list>
    </div>

    <!-- 添加/编辑弹窗 -->
    <van-popup v-model:show="showDialog" position="bottom" round :style="{ height: '80%' }">
      <div class="dialog-content">
        <div class="dialog-header">{{ isEdit ? '编辑商品' : '添加商品' }}</div>
        <van-form @submit="submitProduct">
          <van-field v-model="form.name" label="商品名称" placeholder="请输入商品名称" required />
          <van-field v-model="form.sellPrice" type="number" label="出售价格" placeholder="请输入出售价格" required />
          <van-field v-model="form.purchasePrice" type="number" label="进货价格" placeholder="请输入进货价格" />
          <van-field
            v-model="categoryName"
            label="分类"
            placeholder="选择分类"
            readonly
            @click="showCategoryPicker = true"
            required
          />
          <van-field label="商品图片">
            <template #input>
              <van-uploader 
                v-model="fileList" 
                :max-count="5" 
                :after-read="afterRead"
                @delete="onDeleteImage"
              />
            </template>
          </van-field>

          <div class="spec-section">
            <div class="spec-section-header">
              <span>规格管理</span>
              <van-switch v-model="useSpec" size="20px" />
            </div>
            
            <div v-if="useSpec" class="spec-list">
              <div v-for="(spec, index) in form.specs" :key="index" class="spec-item">
                <div class="spec-inputs">
                  <van-field v-model="spec.name" label="规格名称" placeholder="如: 大杯" dense />
                  <van-field v-model="spec.stock" type="number" label="库存" placeholder="请输入库存" dense />
                </div>
                <van-icon name="close" class="delete-icon" @click="removeSpec(index)" />
              </div>
              <van-button 
                type="primary" 
                size="small" 
                plain 
                block 
                icon="plus" 
                @click="addSpec"
                class="add-spec-btn"
              >
                添加规格
              </van-button>
            </div>
          </div>

          <div class="form-actions">
            <van-button block type="primary" native-type="submit">提交</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 分类选择器 -->
    <van-popup v-model:show="showCategoryPicker" position="bottom">
      <van-picker
        :columns="categoryColumns"
        @confirm="onCategoryConfirm"
        @cancel="showCategoryPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { 
  getProductList, getAllCategories, addProduct, updateProduct, deleteProduct,
  getSpecificationsByProductId, addSpecification, updateSpecification, deleteSpecification 
} from '@/api'
import request from '@/utils/request'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'

const products = ref([])
const loading = ref(false)
const finished = ref(false)

const activeCategory = ref(null)

const showDialog = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  name: '',
  sellPrice: '',
  purchasePrice: '',
  categoryId: null,
  images: [],
  specs: []
})

const useSpec = ref(false)
const originalSpecs = ref([]) // 用于跟踪编辑时的原始规格，以便处理删除

const categories = ref([])
const categoryName = ref('')
const categoryColumns = ref([])
const showCategoryPicker = ref(false)

const fileList = ref([])

onMounted(() => {
  loadCategories()
})

const onTabChange = () => {
  loadProducts()
}

const loadProducts = async () => {
  loading.value = true
  try {
    const data = await getProductList({
      categoryId: activeCategory.value,
      pageNum: 1,
      pageSize: 1000 // 假设管理员想看到所有商品
    })
    products.value = data.records || []
    loading.value = false
    finished.value = true
  } catch (error) {
    console.error('加载商品失败', error)
    loading.value = false
    finished.value = true
  }
}

const getCategoryNameById = (id) => {
  const category = categories.value.find(c => c.id === id)
  return category ? category.name : ''
}

const loadCategories = async () => {
  try {
    categories.value = await getAllCategories()
    categoryColumns.value = categories.value.map(c => ({
      text: c.name,
      value: c.id
    }))
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  showDialog.value = true
}

const editProduct = async (product) => {
  isEdit.value = true
  form.value = {
    id: product.id,
    name: product.name,
    sellPrice: product.sellPrice,
    purchasePrice: product.purchasePrice || '',
    categoryId: product.categoryId,
    images: product.images || [],
    specs: []
  }
  
  // 加载规格
  try {
    const specs = await getSpecificationsByProductId(product.id)
    form.value.specs = specs || []
    originalSpecs.value = JSON.parse(JSON.stringify(form.value.specs))
    useSpec.value = form.value.specs.length > 0
  } catch (error) {
    console.error('加载规格失败', error)
  }
  
  const category = categories.value.find(c => c.id === product.categoryId)
  categoryName.value = category ? category.name : ''
  
  fileList.value = (product.images || []).map((url, index) => ({
    url: url,
    isImage: true
  }))
  
  showDialog.value = true
}

const addSpec = () => {
  form.value.specs.push({
    name: '',
    stock: 0
  })
}

const removeSpec = (index) => {
  form.value.specs.splice(index, 1)
}

const resetForm = () => {
  form.value = {
    id: null,
    name: '',
    sellPrice: '',
    purchasePrice: '',
    categoryId: null,
    images: [],
    specs: []
  }
  useSpec.value = false
  originalSpecs.value = []
  categoryName.value = ''
  fileList.value = []
}

const onCategoryConfirm = ({ selectedOptions }) => {
  form.value.categoryId = selectedOptions[0].value
  categoryName.value = selectedOptions[0].text
  showCategoryPicker.value = false
}

const afterRead = async (file) => {
  file.status = 'uploading'
  file.message = '0%'

  const formData = new FormData()
  formData.append('file', file.file)

  try {
  try {
    const path = await request.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        file.message = `${percentCompleted}%`
      }
    })
    
    file.status = 'done'
    file.message = '上传成功'
    file.url = path
    form.value.images.push(path)
  } catch (error) {
    console.error('上传失败', error)
    file.status = 'failed'
    file.message = '上传失败'
  }
  } catch (error) {
    console.error('上传失败', error)
    file.status = 'failed'
    file.message = '上传失败'
  }
}

const onDeleteImage = (file) => {
  // 从 form.value.images 中移除对应的图片
  const urlToRemove = file.url
  if (urlToRemove) {
    const index = form.value.images.indexOf(urlToRemove)
    if (index !== -1) {
      form.value.images.splice(index, 1)
    }
  }
}

const submitProduct = async () => {
  if (!form.value.name || !form.value.sellPrice || !form.value.categoryId) {
    showToast('请填写必填项')
    return
  }

  // 验证规格
  if (useSpec.value && form.value.specs.length === 0) {
    showToast('请至少添加一个规格')
    return
  }
  if (useSpec.value) {
    for (const spec of form.value.specs) {
      if (!spec.name) {
        showToast('规格名称不能为空')
        return
      }
    }
  }

  try {
    const productData = {
      name: form.value.name,
      sellPrice: parseFloat(form.value.sellPrice),
      purchasePrice: form.value.purchasePrice ? parseFloat(form.value.purchasePrice) : null,
      categoryId: form.value.categoryId,
      images: form.value.images
    }

    let productId = form.value.id
    if (isEdit.value) {
      await updateProduct(productId, productData)
    } else {
      const newProduct = await addProduct(productData)
      productId = newProduct.id // 假设返回了新创建的商品对象
    }

    // 处理规格
    if (useSpec.value) {
      // 1. 处理删除：原始有但现在没有的
      const currentSpecIds = form.value.specs.filter(s => s.id).map(s => s.id)
      const specsToDelete = originalSpecs.value.filter(s => !currentSpecIds.includes(s.id))
      for (const spec of specsToDelete) {
        await deleteSpecification(spec.id)
      }

      // 2. 处理添加和更新
      for (const spec of form.value.specs) {
        const specData = {
          productId: productId,
          name: spec.name,
          stock: parseInt(spec.stock) || 0
        }
        if (spec.id) {
          await updateSpecification(spec.id, specData)
        } else {
          await addSpecification(specData)
        }
      }
    } else if (originalSpecs.value.length > 0) {
      // 如果关闭了规格开关，删除所有现有的规格
      for (const spec of originalSpecs.value) {
        await deleteSpecification(spec.id)
      }
    }

    showSuccessToast(isEdit.value ? '更新成功' : '添加成功')
    showDialog.value = false
    loadProducts()
  } catch (error) {
    console.error('提交失败', error)
    showToast('操作失败')
  }
}

const deleteProductItem = async (id) => {
  showConfirmDialog({
    title: '确认删除',
    message: '确定要删除这个商品吗？'
  }).then(async () => {
    try {
      await deleteProduct(id)
      showSuccessToast('删除成功')
      loadProducts()
    } catch (error) {
      showToast('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.product-manage {
  min-height: 100vh;
  background: #f5f5f5;
}

.product-list {
  padding: 15px;
}

.product-item {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  display: flex;
  gap: 15px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
}

.product-info {
  flex: 1;
}

.product-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.product-category {
  font-size: 12px;
  color: #1989fa;
  background: rgba(25, 137, 250, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
}

.price-row {
  margin-bottom: 5px;
  font-size: 14px;
}

.price-row .label {
  color: #666;
  margin-right: 5px;
}

.price {
  color: #999;
}

.price.sell {
  color: #ff4444;
  font-weight: bold;
}

.product-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.dialog-content {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.dialog-header {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  text-align: center;
}

.form-actions {
  margin-top: 20px;
}

.spec-section {
  margin-top: 20px;
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.spec-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
  font-weight: bold;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f9f9f9;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.spec-inputs {
  flex: 1;
}

.delete-icon {
  color: #ff4444;
  font-size: 20px;
  cursor: pointer;
}

.add-spec-btn {
  margin-top: 10px;
}
</style>
