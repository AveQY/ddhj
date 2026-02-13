<template>
  <div class="manage-layout">
    <router-view />
    
    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/manage/home" icon="chart-trending-o">主页</van-tabbar-item>
      <van-tabbar-item to="/manage/products" icon="goods-collect-o">商品管理</van-tabbar-item>
      <van-tabbar-item to="/manage/categories" icon="apps-o">分类管理</van-tabbar-item>
      <van-tabbar-item to="/manage/orders" icon="orders-o">订单管理</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const active = ref(0)
</script>

<style scoped>
.manage-layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
