# 筷点外卖 · 后端（Spring Boot + MyBatis）

一个外卖点餐平台的服务端，业务分三条线：

- **商品**：分类、菜品（含口味规格）、套餐的管理，以及顾客端的浏览
- **交易**：购物车、下单、支付、订单状态流转、来单与催单的实时推送
- **经营**：优惠券与限时秒杀、员工账号管理、营业数据统计

前端在**同一个仓库**的 [`web/`](web/) 目录（Vue 3 + Vite + Element Plus），
含营销首页、顾客点餐端、商家管理后台三块。

两处实现值得说明：

- **优惠券秒杀**：券带面额与使用门槛。用户端有领券中心（可抢的券 / 我的券）和限时秒杀，
  抢券走 Redis + Lua，在一个脚本里原子完成「判重 + 判库存 + 扣减」；DB 侧用
  `UPDATE ... WHERE stock > 0` 条件更新按影响行数兜底，`(voucher_id, user_id)` 唯一索引
  防同一用户重复领取；落库失败会把 Redis 库存和已购标记一起回滚。下单时可选一张券抵扣，
  核销用条件更新按影响行数判断，防止一张券被两单用掉。
- **接口与鉴权约定**：统一用 `Result` 包装（靠 `code` 判成败，不靠 HTTP 状态码），
  管理端与顾客端各一套 JWT 密钥和请求头，订单金额一律由服务端按购物车计算。
  详见下面「接口约定」一节。

### 仓库结构

| 目录 / 文件 | 内容 |
| --- | --- |
| `sky-common/`、`sky-pojo/`、`sky-server/` | 后端 Maven 三模块 |
| `docs/sky_take_out.sql` | 演示数据库快照（个人信息已全部脱敏） |
| `web/` | 前端工程（Vue 3 + Vite + Element Plus），有独立 README |

---

## 界面预览

**营销首页**（公开页面，纯 CSS + 内联 SVG 实现，不依赖任何图片文件）

![营销首页](web/docs/screenshots/01-home.png)

**顾客点餐端**（挂在 `/order` 下，顾客登录后才能进）

| 点餐 | 我的订单 |
| :---: | :---: |
| <img src="web/docs/screenshots/06-order-menu.png" width="430"> | <img src="web/docs/screenshots/08-order-orders.png" width="430"> |

**商家管理后台**（统一挂在 `/manage` 下，含工作台、订单、菜品、套餐、分类、优惠券、员工、数据统计八个模块）

| 工作台 | 菜品管理 |
| :---: | :---: |
| <img src="web/docs/screenshots/02-dashboard.png" width="430"> | <img src="web/docs/screenshots/03-dish.png" width="430"> |

后台每页顶栏左侧与侧边栏品牌区都能一键回首页：

<img src="web/docs/screenshots/04-back-home.png" width="860">

> 前端的启动步骤、接口对接约定与设计说明见 [`web/README.md`](web/README.md)。

---

## 一、技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Spring Boot 2.7.3、Spring MVC、Spring AOP |
| 持久层 | MyBatis 2.2.0（XML 映射）、PageHelper 1.3.0、Druid 1.2.1 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis（存放店铺营业状态；优惠券秒杀的库存扣减走 Lua 脚本，保证不超卖） |
| 鉴权 | JWT（jjwt 0.9.1），分管理端 / 用户端两套 token |
| 实时通信 | WebSocket（来单提醒、催单） |
| 接口文档 | Knife4j 3.0.2（Swagger 增强） |
| 其它 | Lombok、FastJSON、Apache POI 3.16 |
| 构建 | Maven 多模块，JDK 1.8+ |

### 模块划分

```
sky-take-out
├── sky-common    # 常量、上下文、工具类、异常、公共返回结构 Result / PageResult
├── sky-pojo      # Entity / DTO / VO
└── sky-server    # 启动类、Controller、Service、Mapper、配置、拦截器、WebSocket、定时任务
```

---

## 二、环境要求

| 依赖 | 版本 / 说明 |
| --- | --- |
| JDK | 1.8 及以上 |
| Maven | 3.6+ |
| MySQL | 8.0（端口 3306） |
| Redis | 5.0+（端口 6379） |

---

## 三、快速开始

### 1. 建库并导入演示数据

仓库自带一份可跑的演示数据库快照：`docs/sky_take_out.sql`

```bash
mysql -uroot -p --default-character-set=utf8mb4 < docs/sky_take_out.sql
```

脚本里带了 `DROP DATABASE IF EXISTS sky_take_out`，**会覆盖同名库**，本机已有同名库时注意先备份。

> 这份快照里的顾客姓名、手机号、身份证号、收货地址、微信 openid 已全部替换为虚构演示数据
> （如 `演示顾客` / `13800000001` / `演示大厦 A 座`），不含任何真实个人信息。
> 演示登录账号：商家后台 **admin / 123456**，网页点餐端 **user01 / 123456**
> （另有 user02、user03，密码相同）。

### 2. 生成本地配置

`application-dev.yml` 含数据库密码与云服务密钥，**不在仓库里**，需要从模板复制：

```bash
cd sky-server/src/main/resources
cp application-dev.yml.example application-dev.yml
```

然后按自己的环境改这几项：

```yaml
sky:
  datasource:
    username: root
    password: 你的MySQL密码
  redis:
    password: 你的Redis密码
```

阿里云 OSS（`sky.alioss.*`）与微信小程序（`sky.wechat.*`）**先留占位值也能跑**，
只影响这两块功能，不影响其它功能。

图片上传用的是本机磁盘，不依赖云服务，配置项在 `sky.upload`：

```yaml
sky:
  upload:
    dir: upload       # 落盘目录，相对路径按后端进程的工作目录解析
    url-prefix: /images
```

上传的文件会写到 `sky-server/upload/`，通过 `/images/xxx` 访问。前端 dev server 的
`vite.config.js` 里已经把 `/images` 代理到后端，所以本地开发直接就能看到图。

### 3. 启动 Redis

店铺营业状态存在 Redis 里，**Redis 没起来时 `/admin/shop/status` 会直接返回 500**。

```bash
redis-server
```

### 4. 启动后端

IDE 里直接运行 `sky-server` 模块的启动类 `com.sky.SkyApplication`，或者：

```bash
mvn clean package -DskipTests
java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

服务默认跑在 **8080**。

### 5. 打开接口文档

```
http://localhost:8080/doc.html
```

Knife4j 能正常打开，就说明后端和数据库都通了。

---

## 四、接口约定（对接前端时最容易踩的几个点）

1. **返回结构用 `code` 而不是 HTTP 状态码**

   ```json
   { "code": 1, "msg": null, "data": { } }
   ```

   `code: 1` 成功，`code: 0` 及其它为失败，HTTP 状态码基本都是 200。

2. **鉴权失败返回 HTTP 401**，由 `JwtTokenAdminInterceptor` / `JwtTokenUserInterceptor` 直接 `setStatus(401)`。

3. **两套请求头**：管理端 `/admin/**` 用 `token`，用户端 `/user/**` 用 `authentication`。

4. **分页返回 `PageResult { total, records }`**，不是 `{ total, rows }`。

5. **时间序列化不带秒**：`JacksonObjectMapper` 把 `LocalDateTime` 输出成 `yyyy-MM-dd HH:mm`；
   但查询参数走 `@DateTimeFormat`，订单搜索的 `beginTime / endTime` 要 `yyyy-MM-dd HH:mm:ss`。

6. **删除接口参数名不统一**：菜品是 `@RequestParam List<Long> ids`，套餐是 `@RequestParam List<Long> id`，
   都接收 `1,2,3` 逗号串。

7. **状态由 Service 写死**：新增菜品强制**起售**（`DishServiceImpl.saveWithFlavor`），
   新增套餐强制**停售**（`SetMealServiceImpl.save`），需手动起售。

8. **没有配置 CORS**：`WebMvcConfiguration` 里没有 `addCorsMappings`，
   跨域需要前端用 Vite / Nginx 代理解决。

9. **报表接口返回逗号分隔字符串**，不是数组：

   ```json
   { "dateList": "2026-09-01,2026-09-02", "turnoverList": "406.0,1520.0" }
   ```

10. **WebSocket 地址是 `/ws/{sid}`**，会推两种消息：**`type:1` 是新订单提醒**（在 `paySuccess` 里发）、
    **`type:2` 是顾客催单**（在 `reminder` 里发），格式都是 `{"type":..,"orderId":..,"content":"订单号：.."}`。

---

## 五、免责声明

本项目是个人学习与练手性质的项目，不是生产可用的系统：

- 演示数据为虚构，不含真实个人信息。
- 仓库中不含任何真实的数据库密码、云服务密钥或小程序密钥（真实配置走
  `application-dev.yml`，已被 `.gitignore` 排除）。
- 如用于自己的环境，请自行替换全部密钥并评估安全性。
