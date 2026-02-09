# 简易的商店项目

一个面向移动端的商店小程序，使用 Spring Boot 后端 + Vue 3 前端开发。

by [摸鱼校尉](https://me.aweqy.top/)

## 软件界面
| ![用户界面首页](/image/1.png) | ![购物车界面](/image/2.png) | ![商品详细界面](/image/3.png)       |
|-----------------------|-----------------------|-------------------------------|
| ![确认订单](/image/4.png) | ![后台首页](/image/5.png) | ![商品管理](/image/6.png)         |
| ![分类管理](/image/7.png) | ![订单管理](/image/8.png) | ![订单详细(没截到自行探索)](/image/9.png) |

## 技术栈

### 后端
- Spring Boot 3.1.8
- MyBatis Plus 3.5.9
- MySQL
- Knife4j 4.4.0 (API 文档)
- Fastjson2

### 前端
- Vue 3
- Vite 5
- Vant UI 4 (移动端组件库)
- Vue Router 4
- Axios
- ECharts 5

## 项目结构

```
ddhj/                           # 后端项目
├── src/main/java/com/ddhj/
│   ├── entity/                 # 实体类
│   ├── mapper/                 # MyBatis Mapper
│   ├── service/                # 业务逻辑层
│   ├── controller/             # 控制器
│   ├── config/                 # 配置类
│   └── common/                 # 公共类
└── src/main/resources/
    ├── application.yml         # 应用配置
    └── schema.sql              # 数据库脚本

ddhj-frontend/                  # 前端项目
├── src/
│   ├── views/                  # 页面组件
│   │   ├── UserHome.vue        # 用户主界面
│   │   ├── OrderConfirm.vue    # 订单确认页面
│   │   ├── ManageLayout.vue    # 管理界面布局
│   │   └── manage/             # 管理页面
│   │       ├── Dashboard.vue   # 数据统计
│   │       ├── ProductManage.vue
│   │       ├── CategoryManage.vue
│   │       └── OrderManage.vue
│   ├── router/                 # 路由配置
│   ├── api/                    # API 接口
│   └── utils/                  # 工具类
└── vite.config.js              # Vite 配置
```

## 快速开始

### 1. 数据库准备

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ddhj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构
-- 执行 ddhj/src/main/resources/schema.sql
```

**注意**: 请修改 `application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ddhj?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 123456  # 修改为你的数据库密码
```

### 2. 启动后端

```bash
cd e:\project\ddhj

# 使用 Maven 启动
mvn spring-boot:run

# 或者使用 IDE 运行 DdhjApplication.java
```

后端服务将在 `http://localhost:42835/api` 启动

API 文档地址: `http://localhost:42835/api/doc.html`

### 3. 启动前端

```bash
cd e:\project\ddhj-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:42836` 启动

## 功能说明

### 用户端功能
- **商品浏览**: 左侧分类栏，右侧商品列表展示
- **规格选择**: 点击加号图标，弹出规格选择器（支持库存显示）
- **购物车**: 底部购物栏，支持商品增减
- **下单流程**: 购物车 → 订单确认 → 输入实付金额 → 完成下单

### 管理端功能 (`/manage`)
- **数据统计**: 收入图表（分时/日/月）、今日总收入、热销榜单
- **商品管理**: 添加/编辑/删除商品，支持图片上传，区分进货价和出售价
- **分类管理**: 添加/编辑/删除分类
- **订单管理**: 查看订单列表和详情

## API 接口

### 商品接口
- `GET /api/products` - 商品列表（支持分类筛选）
- `GET /api/products/{id}` - 商品详情
- `POST /api/products` - 添加商品
- `PUT /api/products/{id}` - 更新商品
- `DELETE /api/products/{id}` - 删除商品

### 分类接口
- `GET /api/categories` - 分类列表
- `POST /api/categories` - 添加分类
- `PUT /api/categories/{id}` - 更新分类
- `DELETE /api/categories/{id}` - 删除分类

### 规格接口
- `GET /api/specifications/product/{productId}` - 商品规格列表
- `POST /api/specifications` - 添加规格
- `PUT /api/specifications/{id}` - 更新规格
- `DELETE /api/specifications/{id}` - 删除规格

### 订单接口
- `POST /api/orders` - 创建订单
- `GET /api/orders` - 订单列表
- `GET /api/orders/{id}` - 订单详情

### 统计接口
- `GET /api/statistics/revenue` - 收入统计
- `GET /api/statistics/revenue/today` - 今日总收入
- `GET /api/statistics/hot-products` - 热销商品榜单

### 文件上传
- `POST /api/upload` - 图片上传

## 注意事项

1. **移动端优化**: 前端界面仅针对移动端优化，建议使用浏览器开发者工具的移动设备模拟器查看
2. **图片存储**: 图片保存在项目根目录 `/image/` 文件夹下
3. **端口配置**: 
   - 后端: 42835
   - 前端: 42836
4. **跨域配置**: 已在后端配置 CORS，允许前端访问

## 开发建议

1. 使用 Chrome DevTools 的移动设备模拟器测试界面
2. 推荐使用 iPhone SE 或类似尺寸的设备进行测试
3. 后端 API 文档可通过 Knife4j 查看和测试

## 许可证

MIT
