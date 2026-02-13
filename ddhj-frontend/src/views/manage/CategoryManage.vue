<template>
  <div class="category-manage">
    <van-nav-bar title="分类管理">
      <template #right>
        <van-button type="primary" size="small" @click="showAddDialog">添加</van-button>
      </template>
    </van-nav-bar>

    <div class="category-list">
      <div class="sort-tip">
        <van-icon name="info-o" /> 长按拖拽项目可以更改排序
      </div>
      <draggable 
        v-model="categories" 
        item-key="id"
        @end="onDragEnd"
        :delay="300"
        :delay-on-touch-only="true"
        ghost-class="ghost"
        chosen-class="chosen"
        drag-class="dragging"
        animation="300"
      >
        <template #item="{ element }">
          <div class="category-item">
            <div class="category-info">
              <van-icon name="apps-o" class="drag-handle" />
              <span class="category-name">{{ element.name }}</span>
            </div>
            <div class="category-actions">
              <van-button size="small" @click.stop="editCategory(element)">编辑</van-button>
              <van-button size="small" type="danger" @click.stop="deleteCategoryItem(element.id)">删除</van-button>
            </div>
          </div>
        </template>
      </draggable>
    </div>

    <!-- 添加/编辑弹窗 -->
    <van-dialog
      v-model:show="showDialog"
      :title="isEdit ? '编辑分类' : '添加分类'"
      show-cancel-button
      @confirm="submitCategory"
    >
      <van-field v-model="form.name" label="分类名称" placeholder="请输入分类名称" />
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import draggable from 'vuedraggable'
import { getAllCategories, addCategory, updateCategory, deleteCategory, sortCategories } from '@/api'
import { showToast, showSuccessToast, showConfirmDialog, showLoadingToast } from 'vant'

const categories = ref([])
const showDialog = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  name: '',
  sortOrder: 0
})

onMounted(() => {
  loadCategories()
})

const loadCategories = async () => {
  try {
    categories.value = await getAllCategories()
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', sortOrder: 0 }
  showDialog.value = true
}

const editCategory = (category) => {
  isEdit.value = true
  form.value = { 
    id: category.id, 
    name: category.name,
    sortOrder: category.sortOrder || 0
  }
  showDialog.value = true
}

const submitCategory = async () => {
  if (!form.value.name) {
    showToast('请输入分类名称')
    return
  }

  try {
    const categoryData = { 
      name: form.value.name,
      sortOrder: parseInt(form.value.sortOrder) || 0
    }
    if (isEdit.value) {
      await updateCategory(form.value.id, categoryData)
      showSuccessToast('更新成功')
    } else {
      await addCategory(categoryData)
      showSuccessToast('添加成功')
    }
    loadCategories()
  } catch (error) {
    showToast('操作失败')
  }
}

const deleteCategoryItem = async (id) => {
  showConfirmDialog({
    title: '确认删除',
    message: '确定要删除这个分类吗？'
  }).then(async () => {
    try {
      await deleteCategory(id)
      showSuccessToast('删除成功')
      loadCategories()
    } catch (error) {
      showToast('删除失败')
    }
  }).catch(() => {})
}

const onDragEnd = async () => {
  const toast = showLoadingToast({
    message: '更新排序中...',
    forbidClick: true,
  })
  
  try {
    const ids = categories.value.map(c => c.id)
    await sortCategories(ids)
    toast.close()
    showSuccessToast('排序已保存')
  } catch (error) {
    toast.close()
    showToast('排序保存失败')
    loadCategories() // 恢复原始顺序
  }
}
</script>

<script>
export default {
  components: {
    draggable
  }
}
</script>

<style scoped>
.sort-tip {
  font-size: 13px;
  color: #999;
  text-align: center;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.category-item {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: grab;
  user-select: none;
  touch-action: pan-y;
}

.category-item:active {
  cursor: grabbing;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drag-handle {
  color: #ccc;
  font-size: 20px;
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.category-actions {
  display: flex;
  gap: 8px;
}

/* 拖拽样式 */
.ghost {
  opacity: 0.5;
  background: #e1f0ff !important;
  border: 1px dashed #1989fa;
}

.chosen {
  transform: scale(1.02);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1) !important;
  z-index: 10;
}

.dragging {
  cursor: grabbing;
}
</style>
