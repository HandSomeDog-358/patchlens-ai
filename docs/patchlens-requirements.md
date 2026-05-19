# PatchLens AI 需求文档

## 1. 项目概述

### 1.1 Agent 名称

正式名称：**PatchLens AI**

中文名：**变更透镜**

一句话定位：**一个只在能够证明风险时才评论的 AI PR 审查副驾驶。**

命名理由：

- **Patch** 指向 Pull Request / Merge Request 中的变更本身。
- **Lens** 强调它不是泛泛扫描代码，而是聚焦变更影响面、风险证据和可落地修复。
- 名称适合后续扩展到 GitHub App、GitLab App、Gitee、Gitea、企业私有化部署和 IDE 插件。

备选名称：

- ReviewPilot AI
- DiffSense AI
- CodeSentry AI
- MergeGuard AI

### 1.2 背景

随着 AI 辅助开发变得普遍，代码产出速度大幅提升，但代码审查压力也同步上升。团队需要一个能够理解 PR 变更意图、结合仓库上下文、发现高风险问题并给出可执行建议的审查 Agent。

PatchLens AI 的目标不是替代人类 Reviewer，也不是成为传统 Linter 的替代品，而是承担资深工程师在 PR 审查中最耗时的风险排查、上下文追踪和修复建议工作。

### 1.3 产品定位

PatchLens AI 是面向研发团队的 **Proactive Code Reviewer**。

它的核心原则是：

- **低噪音**：只发布高置信度评论，避免刷屏。
- **强上下文**：结合 PR diff、相关源码、测试、Issue 描述和团队历史模式分析。
- **可执行**：每条关键反馈都尽量包含原因、证据、风险等级和修复建议。
- **建议性**：默认不直接阻断合并，而是帮助人类 Reviewer 更快定位风险。

## 2. 目标与非目标

### 2.1 产品目标

- 自动生成 PR 摘要，帮助 Reviewer 快速理解本次变更。
- 自动识别后端业务代码中的高风险问题。
- 在 PR 行级位置发布高置信度 inline comments。
- 为可修复问题提供 suggested patch。
- 通过项目配置适配不同团队的审查偏好。
- 记录评论采纳、忽略和误报情况，为后续优化提供反馈数据。

### 2.2 MVP 目标

MVP 聚焦 **后端服务 PR 风险审查**，优先支持 GitHub、Gitee 与企业内部 Gitea。

第一版重点发现：

- 权限和认证逻辑缺陷
- 输入校验缺失
- 错误处理回归
- 数据库事务和并发风险
- N+1 查询或明显性能退化
- 破坏性 API 行为变更
- 数据库 schema / migration 风险
- 测试缺失或测试与变更不匹配

### 2.3 非目标

MVP 阶段不做：

- 完整 IDE 插件
- 自动合并 PR
- 全语言全框架深度支持
- 替代 SonarQube / ESLint / Checkstyle 等规则工具
- 大规模自动重构整仓代码
- 对没有证据支撑的代码风格建议进行刷屏式评论

## 3. 用户与场景

### 3.1 目标用户

- 后端开发工程师
- Tech Lead
- 架构师
- DevOps / 平台工程团队
- 需要代码质量治理的中小研发团队

### 3.2 核心使用场景

#### 场景 1：PR 创建后自动审查

开发者创建 PR 后，PatchLens AI 自动接收 Webhook，拉取 PR diff、PR 描述和相关源码，生成 PR 摘要与风险评论。

#### 场景 2：人类 Reviewer 快速理解变更

Reviewer 打开 PR 后先阅读 PatchLens AI 的摘要，了解本次变更涉及的模块、风险点和建议重点审查文件。

#### 场景 3：开发者采纳修复建议

PatchLens AI 在具体代码行留下评论，说明风险原因，并提供可采纳的 suggested patch。

#### 场景 4：团队调整审查策略

项目管理员在前端配置审查强度、风险类型、模型提供商、是否发布 inline comment、是否只生成摘要等策略。

#### 场景 5：审查质量反馈

团队可以标记评论为“有用”“误报”“已采纳”“忽略”，系统记录反馈用于后续改进。

## 4. 功能需求

### 4.1 仓库接入

- 支持 GitHub App 接入。
- 支持 Gitee WebHook 接入。
- 支持使用 Gitee 私人令牌拉取 Pull Request 元信息和变更文件。
- 支持企业内部 Gitea 平台接入，可配置 Gitea API Base URL 和访问令牌。
- 支持配置目标仓库。
- 支持接收 PR opened、synchronize、reopened 事件。
- 支持根据 PR 编号拉取：
  - PR 标题
  - PR 描述
  - changed files
  - diff
  - commit 信息
  - linked issue 信息

MVP 优先 GitHub、Gitee 与 Gitea，后续扩展 GitLab。

### 4.2 PR 上下文构建

系统需要构建一份结构化 Review Context：

- PR 元信息
- diff hunks
- 修改文件完整内容
- 相关调用文件
- 相关测试文件
- 项目目录结构摘要
- 构建文件和依赖信息
- 历史相似代码模式

上下文构建原则：

- 不把整个仓库直接塞给模型。
- 优先检索与变更文件、调用链、测试、配置有关的代码。
- 对大文件进行片段化和摘要化处理。
- 保留每段上下文的文件路径和行号，便于引用证据。

### 4.3 PR 摘要生成

PatchLens AI 需要在 PR 顶部生成摘要，内容包括：

- 本次 PR 做了什么
- 涉及哪些核心模块
- 主要行为变化
- 可能影响的接口、任务、配置或数据表
- 建议人类 Reviewer 重点关注的文件
- 测试覆盖情况初步判断

### 4.4 风险检测

每个风险发现必须包含：

- 风险标题
- 风险等级：critical / high / medium / low
- 置信度：0 到 1
- 具体文件和行号
- 证据说明
- 触发条件或失败场景
- 建议修复方向
- 是否可生成 patch

默认只发布 high confidence 的评论。

MVP 推荐发布阈值：

- critical：置信度 >= 0.70
- high：置信度 >= 0.75
- medium：置信度 >= 0.85
- low：默认不发布 inline comment，只进入报告

### 4.5 行级评论

- 支持在 GitHub PR diff 或 Gitee Pull Request diff 的具体行发布评论。
- 评论语言默认中文，可配置英文。
- 评论必须简洁、具体、可验证。
- 同一风险不得重复评论。
- 对不确定问题使用报告汇总，不发布行级评论。

评论格式示例：

```text
风险：输入校验缺失可能导致越权查询

当前代码直接使用 request.userId 查询订单，但没有确认该 userId 是否属于当前登录用户。
如果调用方传入其他用户 ID，可能读取非本人订单。

建议：使用 authenticatedUserId 替代请求体中的 userId，或增加 ownership 校验。
置信度：0.82
```

### 4.6 Suggested Patch

对高置信度且局部可修复的问题，系统应生成平台可支持的 suggested patch。GitHub 使用 suggestion block；Gitee 若接口能力受限，则先生成普通评论中的修复代码片段。

要求：

- patch 必须尽量小。
- 不跨越无关逻辑。
- 不生成无法编译的伪代码。
- 对涉及业务决策的问题，只给修复方向，不强行生成 patch。

### 4.7 审查策略配置

前端支持配置：

- 是否启用自动审查
- 审查触发事件
- 审查语言
- 风险等级阈值
- 是否发布 inline comments
- 是否生成 suggested patch
- 忽略文件路径
- 重点审查目录
- 模型提供商
- 模型名称
- 最大评论数

### 4.8 审查记录

系统保存每次审查记录：

- PR 信息
- 审查状态
- 使用模型
- token 消耗
- 检索到的上下文摘要
- 发布的评论
- 未发布的候选风险
- 错误日志
- 用户反馈

### 4.9 前端管理台

Vue 前端需要包含：

- 登录页
- 仓库列表
- 仓库接入详情
- 审查策略配置页
- PR 审查记录列表
- 单次 Review 详情页
- 风险评论详情
- 模型与密钥配置
- 基础统计看板

MVP 统计指标：

- 审查 PR 数
- 发布评论数
- 高风险发现数
- 评论采纳数
- 误报数
- 平均审查耗时

## 5. 技术选型

### 5.1 后端

推荐技术栈：

- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.6
- Spring Security
- Spring Data JPA
- PostgreSQL
- pgvector
- Redis
- Flyway
- Docker

Spring AI 版本策略：

- MVP 使用 **Spring AI 1.1.6**。
- 不使用 2.0.0-M 系列作为生产基础版本。
- 后续等 2.x GA 后再评估升级。

Spring AI 主要使用能力：

- ChatClient
- PromptTemplate
- Tool Calling
- VectorStore
- Advisors
- Structured Output
- Observability

### 5.2 前端

推荐技术栈：

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Element Plus 或 Naive UI
- ECharts
- Axios

### 5.3 模型接入

MVP 支持：

- OpenAI compatible API
- 私有化兼容模型网关

后续支持：

- Ollama
- 企业内部模型服务
- 多模型路由
- 按任务选择模型，例如摘要模型、风险检测模型、patch 生成模型

## 6. 系统架构

```text
GitHub / Gitee / Gitea
  -> Webhook Receiver
  -> PR Event Queue
  -> Review Orchestrator
      -> Diff Parser
      -> Repo Context Builder
      -> Code Retriever
      -> Test Mapper
      -> AI Review Engine
      -> Confidence Filter
      -> Comment Publisher
  -> PostgreSQL / pgvector
  -> Redis

Vue Admin Console
  -> REST API
  -> Review Records
  -> Repository Settings
  -> Model Settings
  -> Metrics Dashboard
```

### 6.1 后端模块

- `integration-github`：GitHub App、Webhook、PR API、评论发布。
- `integration-gitee`：Gitee WebHook、Pull Request API、评论发布。
- `integration-gitea`：企业内部 Gitea WebHook、Pull Request API、评论发布。
- `review-orchestrator`：审查任务编排。
- `context-builder`：diff 解析、相关文件检索、上下文压缩。
- `ai-review-engine`：模型调用、提示词、结构化输出。
- `risk-filter`：风险去重、置信度过滤、评论数量控制。
- `patch-generator`：suggested patch 生成和校验。
- `repository-indexer`：代码索引和向量检索。
- `review-record`：审查记录和反馈。
- `admin-api`：前端管理接口。

### 6.2 前端模块

- 登录与权限
- 仓库管理
- 审查策略
- Review 列表
- Review 详情
- 风险详情
- 模型配置
- 数据看板

## 7. 核心数据模型

### 7.1 Repository

- id
- provider
- owner
- name
- defaultBranch
- enabled
- createdAt
- updatedAt

### 7.2 ReviewTask

- id
- repositoryId
- provider
- prNumber
- commitSha
- status
- triggerType
- startedAt
- finishedAt
- errorMessage

### 7.3 ReviewFinding

- id
- reviewTaskId
- severity
- confidence
- filePath
- lineNumber
- title
- description
- evidence
- suggestion
- patch
- published
- providerCommentId

### 7.4 ReviewPolicy

- id
- repositoryId
- language
- minConfidence
- maxInlineComments
- enableSummary
- enableInlineComments
- enableSuggestedPatch
- ignoredPaths
- focusPaths

### 7.5 ModelConfig

- id
- provider
- baseUrl
- modelName
- apiKeyEncrypted
- enabled

## 8. API 草案

### 8.1 仓库

- `GET /api/repositories`
- `GET /api/repositories/{id}`
- `POST /api/repositories/{id}/enable`
- `POST /api/repositories/{id}/disable`
- `GET /api/repositories/{id}/policy`
- `PUT /api/repositories/{id}/policy`

### 8.2 审查任务

- `GET /api/reviews`
- `GET /api/reviews/{id}`
- `POST /api/reviews/{id}/rerun`
- `GET /api/reviews/{id}/findings`

### 8.3 模型配置

- `GET /api/model-configs`
- `POST /api/model-configs`
- `PUT /api/model-configs/{id}`
- `DELETE /api/model-configs/{id}`
- `POST /api/model-configs/{id}/test`

### 8.4 Webhook

- `POST /webhooks/github`
- `POST /webhooks/gitee`
- `POST /webhooks/gitea`

## 9. AI 审查流程

### 9.1 Review Pipeline

```text
1. 接收 PR Webhook
2. 拉取 PR 元数据和 diff
3. 解析 changed files 和 hunks
4. 检索相关代码和测试
5. 生成 PR 摘要
6. 生成候选风险
7. 对候选风险进行证据校验
8. 置信度过滤和去重
9. 生成 suggested patch
10. 发布 PR summary 和 inline comments
11. 保存审查记录
```

### 9.2 结构化输出

AI Review Engine 应要求模型输出 JSON，并由后端进行 schema 校验。

示例：

```json
{
  "summary": "本次 PR 修改了订单查询接口，新增按状态筛选逻辑。",
  "findings": [
    {
      "severity": "high",
      "confidence": 0.82,
      "filePath": "src/main/java/com/example/order/OrderController.java",
      "lineNumber": 42,
      "title": "缺少订单归属校验",
      "evidence": "代码使用请求参数 userId 查询订单，但未与当前认证用户比对。",
      "failureScenario": "攻击者传入其他用户 ID，可能读取非本人订单。",
      "suggestion": "使用当前登录用户 ID，或增加 ownership 校验。",
      "patch": null
    }
  ]
}
```

## 10. 安全与隐私

- GitHub Webhook 必须校验签名。
- Gitee WebHook 必须校验 WebHook 密码或签名配置。
- Gitea WebHook 必须校验 shared secret 或反向代理层签名配置。
- WebHook secret 配置后必须拒绝未携带有效 token 或 HMAC-SHA256 签名的请求。
- API Key 必须加密存储。
- 支持按仓库配置是否允许代码发送到外部模型。
- 企业版需要支持私有模型网关。
- 审查日志不得明文记录完整敏感代码片段。
- 前端需要有基础 RBAC。
- 所有审查任务需要审计记录。

## 11. 非功能需求

### 11.1 性能

- 小型 PR 审查目标耗时：1 到 2 分钟。
- 中型 PR 审查目标耗时：3 到 5 分钟。
- 默认单次 PR 最多发布 5 条 inline comments。

### 11.2 稳定性

- Webhook 处理必须幂等。
- 审查任务支持重试。
- GitHub / Gitee API rate limit 需要可观测。
- 模型调用失败时不影响 PR 正常流程。

### 11.3 可观测性

- 记录每次审查耗时。
- 记录模型调用耗时和 token 用量。
- 记录上下文检索命中情况。
- 记录评论发布成功率。
- 记录用户反馈。

## 12. 验收标准

MVP 完成标准：

- 可以通过 GitHub Webhook、Gitee WebHook 或 Gitea WebHook 自动触发 PR 审查。
- 可以生成 PR summary。
- 可以对 changed files 生成结构化风险发现。
- 可以在 GitHub PR 或 Gitee Pull Request 发布 inline comments。
- 可以在前端查看 Review 记录和 Findings。
- 可以配置仓库审查策略。
- 可以配置至少一个 OpenAI compatible 模型。
- 可以保存评论反馈。
- 审查任务失败可重试。

质量标准：

- 单个 PR 默认评论数不超过 5 条。
- 高风险评论必须包含明确证据。
- 不能发布没有文件路径和行号的 inline comment。
- JSON 输出必须通过 schema 校验后才能入库。
- suggested patch 必须经过基础格式校验。

## 13. 里程碑

### Milestone 1：基础骨架

- Spring Boot 项目初始化
- Vue 项目初始化
- PostgreSQL / Redis / Flyway 配置
- 基础登录和仓库列表
- GitHub / Gitee Webhook 接收

### Milestone 2：PR 摘要

- 拉取 PR diff
- 构建基础 Review Context
- Spring AI 调用模型生成摘要
- 发布 PR summary comment
- 前端展示审查记录

### Milestone 3：风险评论

- 结构化风险输出
- 置信度过滤
- inline comment 发布
- Review findings 入库
- Review 详情页

### Milestone 4：Suggested Patch

- 生成局部修复建议
- GitHub suggestion block / Gitee 修复建议评论
- patch 基础校验
- 用户反馈记录

### Milestone 5：上下文增强

- 代码索引
- pgvector 检索
- 测试文件映射
- 历史相似模式检索

## 14. 成功指标

- PR 平均审查时间减少 30% 以上。
- AI 评论采纳率达到 20% 以上。
- 被标记为误报的评论低于 15%。
- 每个 PR 平均 inline comments 控制在 1 到 5 条。
- 人类 Reviewer 对 PR 摘要有用率达到 70% 以上。

## 15. 初始开发建议

第一版不要追求“全能代码审查”。建议先做：

1. GitHub / Gitee PR summary
2. Java / Spring Boot 后端项目风险审查
3. 高置信度 inline comments
4. Vue 管理台查看审查记录
5. OpenAI compatible 模型配置

当这一版能稳定发现真实问题，并且不会制造太多噪音后，再扩展 GitLab、私有模型、团队规范学习和交互式对话。
