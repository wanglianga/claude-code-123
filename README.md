# 基层法院诉前调解排期与协议履行平台

面向基层法院诉前调解场景的全流程协同平台：当事人提交纠纷 → 立案窗口分流 → 调解中心按案由/专长/可用时间/外地情况排期 → 调解员记录争议焦点与让步 → 线上签署（人脸核验）→ 分期履行跟踪/逾期催办/回访 → 司法确认或转立案。窗口人员、调解员、当事人、法官助理、执行联络人员围绕**同一案件**推进，全过程时间线连续留痕，一键生成可提交法官的连续过程材料包，避免案件在调解与立案之间反复补材料。

## 技术栈

- 后端：Java 17、Spring Boot 3.2、Spring Data JPA、H2（默认，MySQL 兼容模式）/ MySQL 8
- 前端：Vue 3、Vite 5、Element Plus、ECharts 5、Vue Router、Axios
- 默认零外部依赖：使用内存数据库 H2，启动即有演示数据；可一键切换 MySQL

## 目录结构

```
123-a/
├── backend/                Spring Boot 后端（端口 8080）
│   ├── src/main/java/com/court/mediation/
│   │   ├── entity/         12 个领域实体（案件/排期/笔录/协议/分期/签署/送达/回访/转立案/司法确认/导入批次…）
│   │   ├── repo/           Spring Data 仓储
│   │   ├── service/        业务服务（分流排期、调解、协议签署、履行、送达、转立案、统计、导入…）
│   │   ├── controller/     REST 接口（/api/**）
│   │   ├── security/       简化 Token 认证（X-Token 头）
│   │   ├── dto/            案件详情聚合对象
│   │   └── config/         CORS、拦截器、演示数据初始化 DataSeeder
│   └── src/main/resources/ application.yml（H2 默认）、application-mysql.yml、schema-mysql.sql
└── frontend/               Vue3 前端（开发端口 5173，/api 代理到 8080）
    └── src/views、src/components
```

## 本地启动

### 方式一：H2 内存库（推荐，无需安装数据库）

需要 JDK 17+、Maven 3.6+、Node 18+。

```bash
# 终端1：启动后端（首次会下载 Maven 依赖）
cd backend
mvn spring-boot:run

# 终端2：启动前端
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 ，启动时自动写入演示数据（8 个覆盖各状态的案件）。

> 如 npm 提示缓存目录权限问题，可指定临时缓存：`npm install --cache /tmp/npm-cache`

### 方式二：使用 MySQL

```bash
# 1. 建库（可选，脚本仅创建数据库）
mysql -uroot -p < backend/src/main/resources/schema-mysql.sql

# 2. 以 mysql profile 启动后端（表结构由 JPA 自动创建/更新）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql \
  -Dspring-boot.run.arguments="--spring.datasource.username=root --spring.datasource.password=你的密码"
```

### 打包部署

```bash
cd backend && mvn clean package       # 生成 target/mediation-platform.jar
java -jar target/mediation-platform.jar
cd ../frontend && npm run build       # 生成静态文件 frontend/dist，可用 nginx 托管并反代 /api
```

## 测试账号

| 角色 | 账号 | 密码 | 主要权限 |
| --- | --- | --- | --- |
| 管理员（院长） | `admin` | `admin123` | 全部功能、统计看板 |
| 立案窗口 | `window` | `window123` | 登记案件、批量导入、分流、送达 |
| 调解员 | `mediator1` / `mediator2` / `mediator3` | `mediator123` | 排期/改期、调解笔录、协议、签署、回访 |
| 法官助理 | `ja` | `ja123` | 转立案审核、司法确认裁定 |
| 执行联络 | `exec` | `exec123` | 登记收款（含部分履行）、逾期催办、回访 |
| 当事人 | `party1` / `party2` / `party3` | `party123` | 提交纠纷、查看本人案件、签署 |

## 演示数据覆盖的典型场景

1. **案件1 赵建国诉孙磊（民间借贷）**：分期 3 期协议，第一期已付清、第二期部分履行、第三期已逾期；含担保人、电子送达、回访、司法确认申请。
2. **案件2 物业诉李大华（物业费）**：2 次改期、证据缺失、被申请人失联、电子送达——典型拟转立案。
3. **案件3 王芳诉刘强（交通事故）**：外地当事人、特急、线上调解、调解中。
4. **案件4 邻里漏水纠纷**：待分流的紧急案件（可体验专长推荐与分流）。
5. **案件5 建材诉装修公司（小额买卖）**：一次性履行完毕、回访满意、已结案。
6. **案件6 林晓燕诉马涛**：被申请人失联，电子送达推进中。
7. **案件7 冯丽诉郑浩**：协议两期均逾期、催告后转立案，法官助理已登记立案案号。
8. **案件8 排除妨害**：调解不成。

## 建议的验证路径（功能自测）

1. 用 `window` 登录 → **案件管理**，对"待分流"案件点击【分流】，查看按案由匹配的调解员推荐，选择线上/线下并确认。
2. 用 `mediator1` 登录 → 进入案件【调解排期】：排期 → 当事人确认 → 召开完成；再次排期即产生**改期**（版本+1、旧排期取消、案件改期次数累加）。
3. 【调解过程】：录入争议焦点/让步/担保人/履行期限；分别点击【登记被申请人失联】【登记证据材料缺失】【补充证据】，观察案件头部风险标记与时间线。
4. 【协议与履行】：起草协议（可添加多期分期）→ 双方分别【人脸核验并签署】（模拟扫描动画，相似度≥0.90 通过，约15%概率失败可重试）→ 双方签完协议自动生效、案件进入履行中。
5. 用 `exec` 登录：对分期节点【登记收款】（录入小于差额的金额即**部分履行**）、【催告】；点击【立即扫描逾期】验证到期未付节点被标记逾期（每日08:00也有定时任务自动扫描）。
6. 【送达/回访/转立案】页签：发起电子送达（短信/站内/邮件）、新增回访；【生成司法确认材料】后提交，用 `ja` 登录做【裁定】；【申请转立案】选择失联/逾期/多次改期等原因，用 `ja` 审核并登记立案案号。
7. 点击【导出连续过程材料包】，得到含排期改期、笔录、协议签署、履行逾期、送达、回访、完整时间线的文本材料。
8. 【批量导入】页下载 CSV 模板，上传验证批量导入（含失败行明细）。
9. 【统计看板】查看调解成功率、协议履行率、转立案率、案由分布、状态分布、转立案原因、调解员工作量、逾期/失联/缺料预警。

## 核心 REST 接口（均需 `X-Token` 头，登录接口除外）

| 模块 | 方法与路径 |
| --- | --- |
| 认证 | `POST /api/auth/login`、`GET /api/auth/me` |
| 案件 | `GET /api/cases`（状态/案由/调解员/紧急/关键字/失联/缺料/onlyMine）、`POST /api/cases`、`GET /api/cases/{id}`、`POST /api/cases/{id}/triage`、`POST /api/cases/{id}/status` |
| 排期 | `GET /api/schedules/mediators?causeType=`、`POST /api/schedules`（重复调用=改期）、`POST /api/schedules/{id}/confirm`、`POST /api/schedules/{id}/complete` |
| 调解 | `POST /api/mediation/records`、`POST /api/mediation/cases/{id}/unreachable|reached|evidence-missing`、`POST /api/mediation/evidence` |
| 协议/签署 | `POST /api/agreements`（含分期 plans）、`POST /api/agreements/{id}/sign`（facePass 人脸核验） |
| 履行 | `POST /api/performance/plans/{id}/pay`（部分履行）、`/remind`、`POST /api/performance/scan-overdue` |
| 送达/回访 | `POST /api/services`、`POST /api/services/{id}/read`、`POST /api/visits` |
| 司法确认 | `POST /api/judicial/generate/{agreementId}`、`POST /api/judicial/{id}/submit`、`POST /api/judicial/{id}/rule` |
| 转立案 | `POST /api/transfers`、`POST /api/transfers/{id}/review`、`GET /api/transfers/cases/{id}/material-package` |
| 批量导入 | `POST /api/imports/cases`（CSV 文件）、`GET /api/imports/batches` |
| 统计 | `GET /api/stats/overview`、`GET /api/stats/alerts` |
| 用户 | `GET/POST /api/users`、`PUT /api/users/{id}` |

CSV 导入表头：

```
标题,案由,诉求金额,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,证据清单,愿意线上调解(是/否),紧急程度(普通/紧急/特急),是否外地(是/否)
```

## 已执行的验证（实际验证结论）

- `mvn compile` 编译通过；后端以 H2 启动成功，8 个演示案件及账号写入正常。
- 接口实测：登录鉴权（无 Token 返回 400）、案件多条件查询、当事人 onlyMine 数据隔离、CSV 导入（3 行中 2 成功 1 失败且失败原因正确）。
- 五角色全链路实测：当事人提交（案件#11）→ 窗口按专长分流 → 调解员排期 → 录入达成一致的调解笔录 → 起草一次性协议 → 申请人/被申请人人脸核验签署（协议 SIGNED、案件 PERFORMING）→ 生成并提交司法确认、法官助理裁定 CONFIRMED → 执行联络收款后案件 COMPLETED，时间线累计 15 条。
- 修复验证：逾期定时扫描不再把已转立案案件错误改回履行中（案件7 保持 TRANSFERRED）；调解员专长字段正确持久化，交通事故/物业费案由分别匹配到对应专长调解员。
- `npm run build` 前端构建成功（Vite，仅有 chunk 体积提示，不影响运行）；Vite 开发服务器代理 `/api → 8080` 实测打通。

## 说明（演示环境边界）

- 认证为演示用 Base64 Token + 角色拦截模型，生产环境应替换为 JWT/Spring Security 与 HTTPS。
- 人脸核验、活体检测、短信/邮件送达、电子签章 CA 均为**模拟实现**（含扫描动画与相似度评分），对接真实能力时替换 `AgreementService.sign` 与 `ServiceService.serve` 即可。
- H2 为内存库，重启后数据重置（种子数据自动重建）；持久化与多人使用请切换 MySQL profile。
