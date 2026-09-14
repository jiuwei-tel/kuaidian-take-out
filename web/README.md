# 筷点外卖 · 前端（营销首页 + 商家管理后台）

配套后端：**本仓库根目录**（Spring Boot + MyBatis + MySQL + Redis + WebSocket）

技术栈：**Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts**

---

## 界面预览

| 营销首页（纯 CSS + 内联 SVG，不依赖任何图片文件） | 工作台 |
| --- | --- |
| ![营销首页](docs/screenshots/01-home.png) | ![工作台](docs/screenshots/02-dashboard.png) |

| 菜品管理 | 从后台返回首页 |
| --- | --- |
| ![菜品管理](docs/screenshots/03-dish.png) | ![返回首页](docs/screenshots/04-back-home.png) |

---

## 一、快速开始

### 1. 先启动后端

前端所有请求都代理到 `http://localhost:8080`，所以后端必须先跑起来，且依赖以下服务：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| MySQL | `localhost:3306` | 库名 `sky_take_out`，账号 `root / 123456` |
| Redis | `localhost:6379` | 密码 `123456`，`database: 10` |
| 后端 | `localhost:8080` | 启动 `sky-server` 模块的启动类 |

之后可以访问 `http://localhost:8080/doc.html` 确认 Knife4j 接口文档能打开。

### 2. 启动前端

```bash
cd web
npm install
npm run dev
```

浏览器打开终端里输出的地址（默认 `http://127.0.0.1:5173`）。

打开后先看到的是**营销首页**，纯 CSS + 内联 SVG 画的，不依赖任何图片文件。
右上角「管理后台」下拉可以直接进后台各模块；未登录会先跳登录页，登录后再落回原来点的那个模块。

后台统一挂在 `/manage` 下，默认账号 **admin / 123456**（登录页有一键填入按钮）：

| 入口 | 地址 |
| --- | --- |
| 营销首页 | `http://127.0.0.1:5173/#/` |
| 登录 | `http://127.0.0.1:5173/#/login` |
| 商家后台 | `http://127.0.0.1:5173/#/manage/dashboard` |

### 3. 打包

```bash
npm run build     # 产物在 dist/
npm run preview   # 本地预览打包结果
```

---

## 二、和后端的对接约定（踩过的坑都在这）

这套后端有几个不那么「常规」的地方，前端都做了对应处理，改代码前建议先看一眼：

### 1. 统一返回结构不是 HTTP 状态码

后端的 `Result` 约定是 **`code: 1` 成功、`code: 0` 及其它失败**，HTTP 状态码基本都是 200。

```json
{ "code": 1, "msg": null, "data": { ... } }
```

`src/api/request.js` 的响应拦截器统一判断 `code === 1`，成功时直接把 `data` 返回给页面，失败时弹 `msg` 并 reject。

### 2. 401 是登录态失效

`JwtTokenAdminInterceptor` 校验 token 失败时返回 **HTTP 401**（不是 `code: 0`）。拦截器捕获 401 后会清掉本地 token 并跳回登录页。

### 3. 管理端 token 的请求头名字叫 `token`

```java
sky.jwt.admin-token-name: token
```

用户端用的是 `authentication`，本后台只用管理端，所以固定发 `token`。

### 4. 时间格式不带秒

`JacksonObjectMapper` 把 `LocalDateTime` 序列化成 **`yyyy-MM-dd HH:mm`**（注意：没有秒）。前端 `utils/format.js` 的 `fmtDateTime` 只做空值兜底和 `T` 替换，**不会**再转成 `Date` 重新格式化，否则时区会把时间改掉。

但订单搜索的 `beginTime / endTime` 走的是 query 参数 + `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")`，所以要带秒。

### 5. 分页是 `PageResult { total, records }`

不是 `{ total, rows }`，也不是 MyBatis-Plus 的 `{ total, list }`。

### 6. 菜品删除的参数名是 `ids`，套餐删除是 `id`

```java
// DishController
public Result delete(@RequestParam List<Long> ids)
// SetMealController
public Result delete(@RequestParam List<Long> id)
```

而且 Spring 接的是 `ids=1,2,3` 这种逗号串，不是 `ids[]=1&ids[]=2`。`src/api/dish.js` 和 `src/api/setmeal.js` 里已经手动 `join(',')` 了。

### 7. 新增菜品强制起售，新增套餐强制停售

后端 Service 里写死的：

```java
// DishServiceImpl.saveWithFlavor
dish.setStatus(StatusConstant.ENABLE);     // 起售

// SetMealServiceImpl.save
setmeal.setStatus(StatusConstant.DISABLE); // 停售
```

所以新增套餐后一定要手动点「起售」，页面上的成功提示也写了这一点。

### 8. 报表接口返回的是逗号分隔字符串

```json
{ "dateList": "2026-09-01,2026-09-02", "turnoverList": "406.0,1520.0" }
```

用 `utils/format.js` 的 `splitList / splitNumList` 拆。

### 9. 跨域靠 Vite 代理解决

后端 `WebMvcConfiguration` 里**没有配置 CORS**，所以前端不能直连 8080。`vite.config.js` 里把 `/admin` 和 `/ws` 都代理到了 `http://localhost:8080`，前端代码里直接写 `/admin/xxx` 即可。

WebSocket 也走同一个代理（`ws: true`），地址是 `ws://127.0.0.1:5173/ws/{sid}` → 转发到后端的 `/ws/{sid}`。

### 10. 图片上传依赖阿里云 OSS

`/admin/common/upload` 走的是 `application-dev.yml` 里配置的 `sky.alioss.*`。如果那对 access-key 已经失效，上传会失败 —— 上传组件会自动降级成「手填图片地址」，不会卡住你录数据。

### 11. 菜品图片：教学用的 OSS bucket 已经失效

库里 `dish.image` 存的大多是黑马官方的 bucket：

```
https://sky-itcast.oss-cn-beijing.aliyuncs.com/xxxx.png
```

这个 bucket 现在返回 **403 AccessDenied**（教学资源权限已收回），所以菜品列表整片显示不出图 —— 这**不是前端问题**，浏览器直接打开那个链接同样 403。

本项目的处理方式：

1. **`public/dishes/` 放了 3 张本地示意图片**，`dish.image` 直接填相对路径 `/dishes/xxx.jpg`（Vite 会把 `public/` 挂到根路径，所以 `/dishes/xxx.jpg` 就能取到）。当前已换上：

   | 菜品 | id | image |
   | --- | --- | --- |
   | 馋嘴牛蛙 | 64 | `/dishes/frog.jpg` |
   | 江团鱼2斤 | 66 | `/dishes/fish-steamed.jpg` |
   | 鸡蛋汤 | 68 | `/dishes/soup-egg.jpg` |

2. **其余菜品保持原样**（地址仍然失效）。为了不让列表里冒出 Element Plus 默认的「加载失败」字样，所有 `el-image` 都加了 `#error` 降级槽，加载失败时渲染成和无图状态一致的虚线占位。

3. **要自己补齐**：把图片文件丢进 `public/dishes/`，再把 `dish.image` 改成 `/dishes/文件名`。用 SQL 最快：

   ```sql
   UPDATE dish SET image = '/dishes/你的图.jpg' WHERE id = 64;
   ```

   走后台的「修改菜品」弹窗也行，就是每张都要点一次。

> 这 3 张图取自 [TheMealDB](https://www.themealdb.com/)（开放的菜品图库），仅作课程演示用。

---

## 三、功能清单

| 模块 | 路由 | 对应后端接口 |
| --- | --- | --- |
| 营销首页 | `/` | 无（纯展示），右上角「管理后台」下拉进各模块 |
| 登录 | `/login` | `POST /admin/employee/login` |
| 工作台 | `/manage/dashboard` | `/admin/workspace/businessData`、`overviewOrders`、`overviewDishes`、`overviewSetmeals` |
| 订单管理 | `/manage/order` | `/admin/order/conditionSearch`、`statistics`、`details/{id}`、`confirm`、`rejection`、`delivery/{id}`、`complete/{id}`、`cancel` |
| 菜品管理 | `/manage/dish` | `/admin/dish` 增删改查、`/status/{status}`、`/list` |
| 套餐管理 | `/manage/setmeal` | `/admin/setmeal` 增删改查、`/status/{status}` |
| 分类管理 | `/manage/category` | `/admin/category` 增删改查、`/status/{status}`、`/list` |
| 员工管理 | `/manage/employee` | `/admin/employee` 增改查、`/page`、`/status/{status}` |
| 数据统计 | `/manage/statistics` | `/admin/report/turnoverStatistics`、`userStatistics`、`ordersStatistics`、`top10` |

另外有两处全局能力：

- **营业状态开关**（顶栏）：`GET /admin/shop/status`、`PUT /admin/shop/{status}`，状态存在 Redis 的 `SHOP_STATUS` 里。切换会弹确认框，因为误触会直接影响顾客下单。
- **来单提醒**：连接 `ws://…/ws/{sid}`，收到 `{type: 2, orderId, content}` 时右下角滑出一张小票、播放提示音，点「去接单」直接跳到待接单列表。WebSocket 断了会按 1s→1.6 倍退避自动重连，最长 30s。

---

## 四、目录结构

```
src/
├── api/            # 每个后端 Controller 对应一个文件
│   ├── request.js  # axios 实例 + code/401 拦截器
│   └── ...
├── components/
│   └── ImageUpload.vue     # 图片上传（失败可降级手填地址）
├── constants/index.js      # 订单状态、分类类型等枚举口径
├── layout/                 # 侧边栏 / 顶栏 / 主框架 + WebSocket
├── router/index.js         # hash 路由 + 登录守卫（首页公开，后台挂 /manage）
├── stores/                 # pinia：user / shop / notice
├── styles/
│   ├── tokens.css          # 设计令牌（颜色、字体、尺寸）
│   └── index.css           # 全局基础样式 + Element Plus 校准
├── utils/                  # auth / format / sound
└── views/                  # 各页面（home/ 是营销首页，其余是后台页面）
```

---

## 五、设计说明

视觉基调叫「**出单机**」——商家后厨那台出单机的质感：

- **骨架**用灶青黑 `#16211F`（青砖铁锅），**内容底**用热敏纸灰白 `#F4F5F2`，不用纯白也没有渐变。
- **品牌黄** `#FFC200` 是筷点外卖的品牌色，但它很克制：只出现在侧边栏当前项的立标、营业中指示灯、以及销量榜第一名。用得越少越有分量。
- **主色**是灶青 `#1E5A4E`，按钮、链接、选中态都用它。
- **灶火橙红** `#D9480F` 只留给真正需要动作的地方：待接单角标、来单提醒、行内「取消/拒单」。颜色在这里是信息，不是装饰。
- **数字**（金额、订单号、数量）一律用等宽字体 + `tabular-nums`，右对齐，因为这是商家每天要反复扫读的东西。
- 工作台的今日数据是一条带细分隔线的横带，不是五张渐变卡片；订单详情和来单提醒用了「小票」的锯齿边和虚线齿孔，这是这个行业真实的物料。
- **营销首页**沿用同一套令牌，但把品牌黄铺成满版头图区 —— 全页只有这一处大面积用色。页面刻意不用卡片：分栏靠发丝线，菜单用「点线引导符 + 等宽价格」排成真菜单的样子，头图区那台手机是纯 CSS 画的，连菜品缩略图都是内联 SVG 图标。整个首页没有一个图片文件、没有一次外部请求。

改配色只需要动 `src/styles/tokens.css` 一个文件。

---

## 六、已知限制

- **用户端（C 端）不在本后台范围内**。后端的 `/user/user/login` 要真实的微信小程序 `code` 换 openid，`/user/order/payment` 走微信支付，本地环境都跑不通。所以首页上的「立即点餐」不会跳到点单页，它是滚到页面内的示例菜单 —— 目前**没有真的顾客点单端**。
- 图表用的是 ECharts 全量引入，包体积偏大，课设够用；要优化可以换成按需引入。
- 路由用 hash 模式，好处是部署到 Nginx 不需要额外配 `try_files`。
