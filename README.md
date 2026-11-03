# 青途 · 校园校车智能调度系统

基于《校园校车智能调度系统需求分析文档》实现的全栈项目，包含学生端、司机端、管理端与后端服务。

## 技术栈

| 层面 | 选型 |
| --- | --- |
| 学生端 / 司机端 | uni-app (Vue 3) + uni-ui，H5 与小程序均可运行 |
| 管理端 Web | Vue 3 + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.3.x |
| 数据库 | MySQL 8.x |
| ORM | MyBatis-Plus 3.5.x |
| 认证 | JWT（登录返回 token，请求头 `Authorization: Bearer <token>`，拦截器校验角色） |
| API 文档 | Knife4j / OpenAPI 3 |

## 目录结构

```
Project-T/
├── frontend/
│   ├── student-app/       # 学生端 uni-app，端口 5174
│   ├── driver-app/        # 司机端 uni-app，端口 5175
│   └── admin-web/         # 管理端 Vue3 + Element Plus，端口 5173
├── backend/               # Spring Boot 后端，端口 8080
│   └── src/main/java/com/qingtu/
│       ├── config/        # JWT、跨域、MyBatis-Plus、Knife4j 配置
│       ├── controller/    # 控制层
│       ├── service/       # 业务层
│       ├── mapper/        # 数据访问层
│       ├── entity/        # 实体类
│       ├── dto/           # 数据传输对象
│       ├── common/        # Result、异常、枚举、角色注解
│       └── scheduler/     # 模拟车辆位置定时任务
├── sql/
│   └── schema.sql         # 建表 + 演示数据
└── README.md
```

## 环境要求

- JDK 17 及以上
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8.x

## 1. 初始化数据库

```sql
CREATE DATABASE campus_bus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

在项目根目录执行：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p campus_bus < sql/schema.sql
```

说明：

- 脚本会创建 9 张核心表：`sys_user`、`bus_route`、`bus_stop`、`bus_vehicle`、`bus_shift`、`bus_reservation`、`bus_vehicle_location`、`dispatch_suggestion`、`driver_task`。
- 演示班次使用 `CURDATE()` 生成日期，建议在演示当天重新导入，保证有“今日班次”。
- 所有演示账号密码均为 `123456`（BCrypt 存储）。

## 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认连接 `jdbc:mysql://localhost:3306/campus_bus`，用户名 `root`，密码 `123456`。可通过环境变量覆盖：

```bash
DB_URL="jdbc:mysql://localhost:3306/campus_bus?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" \
DB_USERNAME=root \
DB_PASSWORD=your_password \
mvn spring-boot:run
```

- 服务地址：`http://localhost:8080`
- 接口文档：`http://localhost:8080/doc.html`

## 3. 启动前端

### 管理端 Web（5173）

```bash
cd frontend/admin-web
npm install
npm run dev
```

访问 `http://localhost:5173`，Vite 已将 `/api` 代理到 `http://localhost:8080`。

### 学生端（5174）

```bash
cd frontend/student-app
npm install
npm run dev:h5
```

访问 `http://localhost:5174`。

### 司机端（5175）

```bash
cd frontend/driver-app
npm install
npm run dev:h5
```

访问 `http://localhost:5175`。

真机 / 小程序调试时，请将 `VITE_API_BASE` 指向后端可访问地址（例如 `http://192.168.x.x:8080/api`），或在对应 `vite.config.js` 中调整代理。

## 演示账号

| 端 | 账号 | 密码 | 角色 |
| --- | --- | --- | --- |
| 管理端 | `admin` | `123456` | ADMIN |
| 学生端 | `student` | `123456` | STUDENT |
| 司机端 | `driver01` | `123456` | DRIVER |

其他学生账号 `student02` ~ `student15`，司机账号 `driver02`，密码同为 `123456`。

登录时前端会携带角色，后端校验账号角色不匹配时拒绝登录；JWT 中携带 `userId/name/role`，接口通过 `@RequireRole` 做三端隔离，学生访问管理接口返回 403。

## 核心流程

### 学生端

1. 登录 / 注册。
2. 班次查询：按日期、线路筛选今日/明日班次，展示余座与状态。
3. 班次详情：站点顺序、车辆、司机、余座；余座充足时可预约。
4. 我的预约：查看预约记录、取消预约（车辆发车后不可取消）。
5. 车辆位置：班次运行中时每 10 秒轮询位置、当前站/下一站、ETA 与站点进度。

### 司机端

1. 登录后查看分配给自己的任务（待执行 / 执行中 / 已完成）。
2. 任务详情展示线路站点、车辆、预约人数。
3. 开始任务：班次与车辆切换为运行中，并初始化车辆位置。
4. 运行中可标记延误 / 恢复正常，可结束任务。
5. 运行中每 10 秒刷新车辆位置与进度。

### 管理端

- 仪表盘：班次、预约、学生、车辆、平均上座率统计。
- 基础数据：线路、站点、车辆、班次的增删改查。
- 班次管理：分配司机与车辆，自动同步司机任务。
- 预约管理：按班次/日期查看预约明细。
- 统计报表：线路预约分布、班次上座率。
- 调度建议：规则生成建议、确认执行、忽略。
- 车辆监控：查看运行中车辆的实时位置与进度。

## 模拟位置与实时性

- 司机点击“开始任务”后，后端初始化 `bus_vehicle_location`，车辆位于线路首站。
- 定时任务每 30 秒执行一次（`app.simulation.rate`），按 `elapsed / seconds-per-segment` 推进区间进度，并在相邻站点经纬度之间线性插值。
- 每经过 `seconds-per-segment`（默认 45 秒）到达下一站，直至终点后状态置为 `ARRIVED`。
- 学生端与司机端均采用前端轮询（10 秒），不引入 WebSocket 与 Redis，适合课程项目演示。

可调整 `backend/src/main/resources/application.yml`：

```yaml
app:
  simulation:
    rate: 30000            # 定时任务间隔（毫秒）
    seconds-per-segment: 45 # 每个站点区间所需秒数
```

## 智能调度规则

系统按班次预约率生成调度建议，同一天同一班次同类型建议处于待处理状态时不会重复生成：

| 条件 | 建议类型 | 含义 |
| --- | --- | --- |
| 预约率 > 95% | `ADD_VEHICLE` | 增加车辆 |
| 预约率 80% - 95% | `LARGER_VEHICLE` | 更换更大容量车辆 |
| 预约率 < 30% | `REDUCE_CAPACITY` | 更换小容量车辆或合并班次 |
| 相邻班次预约率差异 > 40% | `REALLOCATE` | 运力重新分配 |
| 班次车辆不可用 | `REASSIGN_VEHICLE` | 重新分配可用车辆 |

管理员在“调度建议”页可一键生成、确认或忽略建议。确认换车时，系统会校验新车容量不低于已预约人数，并同步更新班次与司机任务。

## 主要接口

| 模块 | 路径前缀 | 角色 |
| --- | --- | --- |
| 认证 | `/api/auth` | 公开（`/login`、`/register`） |
| 学生端 | `/api/student` | STUDENT |
| 司机端 | `/api/driver` | DRIVER |
| 管理端 | `/api/admin` | ADMIN |

完整接口与参数说明见 Knife4j：`http://localhost:8080/doc.html`。
