# 简易店铺小程序 - 前端

基于 Vue 3 + Vant UI 的移动端简易店铺前端

by [摸鱼校尉](https://me.aweqy.top/)

## 安装依赖

```bash
npm install
```

## 开发

```bash
npm run dev
```

- 用户：访问 http://localhost:42836
- 管理员：http://localhost:42836/manage
（自行添加登录功能）

## 构建

```bash
npm run build
```

## 页面路由

- `/` - 用户主界面（商品浏览、购物车）
- `/order-confirm` - 订单确认页面
- `/manage` - 管理界面
  - `/manage/home` - 数据统计
  - `/manage/products` - 商品管理
  - `/manage/categories` - 分类管理
  - `/manage/orders` - 订单管理

## 技术栈

- Vue 3
- Vite
- Vant UI 4
- Vue Router
- Axios
- ECharts
