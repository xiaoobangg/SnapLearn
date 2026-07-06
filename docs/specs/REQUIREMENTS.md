# SnapLearn（拍立学）业务需求文档
## 文档说明
本文档为 **SnapLearn 拍立学最终版业务需求**，以**卡片组**为核心学习单元，完整覆盖分步学习、循环重学、多题型测试、全对通关等核心规则，可直接用于产品评审、前后端开发、测试用例编写。
---
## 一、用户画像
| 画像 | 描述 | 核心诉求 |
|------|------|---------|
| 英语学习者 | 有英语学习需求的学生/职场人士 | 随时随地积累词汇，按组学习、科学巩固 |
| 阅读者 | 经常阅读英文材料（论文/新闻/书籍） | 遇到生词快速组卷记录，按组学习不零散 |
| 备考者 | 准备英语考试（四六级/考研/雅思/托福） | 真题生词按组归集，学测结合高效通关 |
| 开发者/集成方 | 希望通过 API 集成拍立学能力 | 简单可靠的 API Key 认证 + 标准接口 |
---
## 二、核心概念定义（开发统一口径）
1. **卡片组**：每次拍照→勾选单词生成的学习单元，1次拍照=1个卡片组，是学习/测试的最小维度
2. **卡片**：1个单词在卡片组中的关联记录，内容引用 word_contents
3. **知识点**：卡片内最小学习单元（单词、音标、释义、例句、记忆技巧、词性），分步逐一展示
4. **词库**：每日打卡的单词来源，支持管理员预置和用户自行导入（从卡片组导入）
5. **打卡池**：用户每日打卡的个性化进度，独立于卡片组数据
6. **word_contents**：单词的结构化内容（LLM 生成），独立存储，卡片组和打卡池共同引用
7. **错题本**：按卡片组归集测试答错的卡片，支持定向重学
8. **ReactAgent**：基于 Spring AI Alibaba 的智能体，支持对话式创建卡片组
9. **长期记忆**：用户级个性化信息存储（考试目标、英语水平、学习偏好），跨会话保留
10. **API Key**：用户自服务生成的访问密钥，用于外部系统集成（如 Coze 插件）
---
## 三、核心用户旅程 
 拍照选词 → 生成卡片组 → 知识点分步学习 → 卡片组学完 → 多题型测试 → 错题归集 → 整套重测通关 
1. 拍照选词：用户拍照 / 从相册选图，OCR 识别英文并提取单词，勾选目标生词 
2. 生成卡片组：勾选确认后，自动创建 1 个卡片组，状态为待学习 
3. 知识点分步学习：以卡片组为单元，逐卡、逐知识点展示，用户手动点击确认切换，本轮学完后循环重学标记「需再学」的卡片 
4. 卡片组学完：组内所有卡片均标记「已掌握」，卡片组状态变为学习完成，可进入测试 
5. 多题型测试：针对学习完成的卡片组，随机分配 4 种题型（释义选择、单词选择、组词搭配、单词拼接），逐题作答，答错不实时提示 
6. 错题归集：测试全部完成后统一批改，错题自动归入本组错题本，支持定向重学错题卡片 
7. 整套重测通关：未全对需重新测试整套题目，全部答对则卡片组状态变为测试完成，学习闭环结束
 
### 场景一：拍照生成卡片组
1. 用户阅读遇到生词，打开拍立学小程序
2. 拍照/相册选图 → 百度OCR识别英文 → 智能提取单词
3. 用户勾选目标单词 → 自动生成**1个卡片组**，状态：待学习

### 场景二：卡片组分步学习
1. 选择待学习卡片组，进入学习模式
2. 按顺序展示单张卡片，**逐知识点展示**
3. 知识点必须**用户点击确认**才切换下一个
4. 单卡知识点学完，用户标记：已掌握/需再学
5. 本轮所有卡片学完，**统一循环重学「需再学」卡片**
6. 全卡标记已掌握 → 卡片组状态：学习完成，可进入测试

### 场景三：卡片组测试通关
1. 对学习完成的卡片组发起测试
2. 系统随机分配**4种题型**，逐题作答（答错不实时提示）
3. 全部答完统一批改，**错题自动归入本组错题本**
4. 用户可选择：重学错题卡片 / 直接重测整套题
5. **必须整套测试全部答对** → 卡片组状态：测试完成（学习闭环）
6. 未全对 → 保留测试中状态，需反复重测整套题

### 场景四：学习本管理
1. 按卡片组展示所有学习内容，按状态筛选
2. 查看卡片组学习/测试进度、错题列表
3. 定向重学错题卡片、重新测试未通关卡片组
4. 查看已通关卡片组，支持回顾复习

### 场景五：每日打卡
1. 用户选择打卡词库，设置每日新词量/复习量
2. 每日打开打卡页面，系统推送 N 个新词 + 待复习旧词
3. 逐词学习（发音+释义+例句），用户标记：认识/模糊/不认识
4. 系统根据标记排期下次复习时间（认识→间隔拉长，不认识→短期再现）
5. 当日打卡完成后，用户可勾选当日单词创建卡片组或加入已有卡片组
6. 卡片组的单词学习完成后，可主动导入到打卡词库中长期复习

### 场景六：AI 对话创建卡片组
1. 用户进入 AI 对话页面，输入文本描述（如"帮我创建一篇关于气候变化的卡片组"）
2. AI 智能体自动提取单词、查重（告知哪些已学过）、推荐相关词
3. 用户确认最终词表和标题后，智能体自动创建卡片组
4. 用户可随时撤销最近创建的卡片组
5. AI 记住用户的个性化信息（考试目标、英语水平），后续对话自动应用

---
## 四、功能模块需求
### 1. 用户认证模块
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 微信手机号登录 | 通过微信授权获取手机号，一键注册/登录 | P0 |
| 微信 OpenID 登录 | 无手机号时允许通过 OpenID 登录（phone 可空） | P0 |
| 开发测试登录 | DEBUG 模式下通过手机号+验证码直接登录 | P1 |
| JWT Token 管理 | Token 过期自动跳转登录页 | P0 |

### 2. 拍照识别模块（卡片组创建）
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 拍照/相册上传 | 调用手机相机拍照或从相册选择图片 | P0 |
| OCR 文字识别 | 调用百度 OCR API 识别图片中的英文文字 | P0 |
| 单词提取 | 从识别结果中智能提取英文单词，去重过滤 | P0 |
| 多选勾选 | 用户自由勾选/取消需要学习的单词 | P0 |
| 卡片组生成 | 勾选确认后，自动创建1个卡片组，绑定所选单词 | P0 |

### 3. AI 卡片生成模块
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 一键生成卡片 | 用户确认选词后，调用 LLM 批量生成学习卡片 | P0 |
| 卡片内容结构化 | 每张卡片包含：单词、音标、词性、释义、语境含义、例句、记忆技巧 | P0 |
| 知识点拆分 | 卡片内容按独立知识点拆分，支持分步展示 | P0 |
| 生成进度反馈 | 生成过程中展示 loading 状态 | P1 |

### 4. 卡片组学习模块
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 按组学习 | 以卡片组为单元发起学习，不支持单卡零散学习 | P0 |
| 知识点分步展示 | 单卡内知识点逐次展示，**用户点击确认才切换下一个** | P0 |
| 卡片掌握标记 | 单卡学完后，用户标记：已掌握/需再学 | P0 |
| 循环重学 | 本轮所有卡片学完后，统一重学「需再学」卡片 | P0 |
| 学习完成判定 | 卡片组内所有卡片均标记「已掌握」，标记为学习完成 | P0 |
| 学习进度展示 | 显示当前卡片组：已学卡片数/总卡片数 | P1 |
| 错题重学 | 测试后可针对本组错题卡片，单独发起重学 | P0 |

### 5. 卡片组测试模块（新增核心）
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 测试触发 | 仅「学习完成」状态的卡片组可进入测试 | P0 |
| 4种题型随机出题 | 每套测试随机分配以下题型，1张卡片对应1道题<br>1.释义选择题（看单词选释义）<br>2.单词选择题（看释义选单词）<br>3.组词搭配题<br>4.单词拼接/拼写题 | P0 |
| 测试交互 | 逐题作答，答错不实时提示，答完统一提交批改 | P0 |
| 错题自动归集 | 测试完成后，所有答错卡片自动归入本组错题本 | P0 |
| 错题重学入口 | 支持直接对测试错题卡片发起定向重学 | P0 |
| 整套重测规则 | 未全对必须**重新测试整套题目**，不支持只测错题 | P0 |
| 通关判定 | 整套题目**全部答对**，标记卡片组测试完成（通关） | P0 |
| 测试结果展示 | 展示正确率、错题列表、正确答案解析 | P1 |

### 6. 学习本模块
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 卡片组列表 | 分页展示所有卡片组，展示状态、进度、通关标识 | P0 |
| 状态筛选 | 按卡片组状态筛选：待学习/学习中/学习完成/测试中/测试完成 | P0 |
| 错题本查看 | 按卡片组查看测试错题卡片，支持快速重学 | P0 |
| 卡片组操作 | 快捷入口：开始学习、进入测试、重学错题、回顾内容 | P0 |
| 卡片学习历史 | 查看单张卡片的学习次数、测试对错记录 | P1 |

### 7. 个人中心模块
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 用户信息展示 | 显示头像、昵称、手机号 | P1 |
| 学习概览 | 总卡片组数、学习完成数、测试通关数、待测卡片组数、错题总数 | P0 |
| 快捷入口 | 待学习卡片组、待测试卡片组、错题本快速跳转 | P0 |
| API Key 管理 | 创建/查看/撤销 API Key（用于外部集成） | P1 |

### 8. 每日打卡模块（新增）

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 词库选择 | 用户选择打卡词库（默认从卡片组导入的词库 + 后续预置词库） | P0 |
| 每日推送 | 系统按用户设置推送 N 个新词 + M 个待复习旧词 | P0 |
| 单词学习 | 逐词展示，复用 word_contents 结构化数据（发音+释义+例句），用户标记：认识/模糊/不认识 | P0 |
| 复习排期 | 根据标记自动排期：认识→间隔×2 延后，模糊→间隔不变，不认识→间隔重置为1天 | P0 |
| 打卡记录 | 记录每日打卡日志（日期、新词数、复习词数、标记分布） | P1 |
| 打卡日历 | 可视化展示月度打卡日历（连续天数、打卡记录） | P1 |
| 从打卡创建卡片组 | 当日打卡完成后，用户勾选单词创建新卡片组或加入已有卡片组 | P0 |
| 卡片组导入打卡池 | 卡片组学习通关后，用户可主动将组内单词导入打卡词库，长期复习 | P0 |
| 用户设置 | 每日新词量（默认10）、每日复习量（默认20）、打卡提醒开关、聊天偏好设置 | P0 |

### 9. AI 对话模块

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 流式对话 | SSE 流式逐字输出，实时打字效果 | P0 |
| 同步对话 | 普通 POST 请求，一次性返回完整回复 | P1 |
| 多模型切换 | 支持 DeepSeek / 通义千问切换 | P0 |
| 会话记忆 | 基于 chatId 的对话历史，自动注入 LLM 上下文 | P0 |
| 会话管理 | 历史会话列表、切换会话、删除会话 | P1 |
| 知识库增强 | RAG 模式，基于上传文档回答问题，按用户隔离 | P1 |
| 多轮压缩 | 长对话自动压缩历史为独立查询，避免代词污染检索 | P1 |
| 查询重写 | 清洗冗余客套，精简成检索友好查询 | P1 |
| 多查询扩展 | 一查多变体，提升召回率 | P1 |
| 空召回拒答 | 检索 0 条时显式告知"无相关资料"，避免幻觉 | P0 |
| **Agent 模式** | **ReactAgent 智能体，对话式创建卡片组** | P0 |
| **长期记忆** | **保存/读取用户个性化信息（考试目标、英语水平等）** | P1 |
| **单词查重** | **Agent 自动查询用户已学单词，避免重复学习** | P0 |
| **单词推荐** | **基于种子词推荐语义相关单词，扩展词表** | P1 |
| **撤销创建** | **支持撤销最近创建的卡片组** | P1 |
| Admin 端入口 | 管理后台亦提供 AI 对话页面，与小程序端方案一致 | P1 |

### 10. RAG 知识库模块

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 文档上传 | 支持 PDF / DOCX / PPTX / TXT / MD / HTML / CSV / JSON | P1 |
| 自动分块 | Tika 解析 → TokenTextSplitter 分块 | P1 |
| MD 特殊处理 | Markdown 用 `MarkdownDocumentReader` 按标题分块 | P2 |
| 向量化存储 | DashScope text-embedding-v4（1536 维）→ pgvector | P1 |
| 用户级过滤 | 检索时按 metadata `user_id` 过滤，避免数据串户 | P0 |
| 对话增强 | RAG 流水线自动检索相似片段注入 prompt | P1 |
| 文档管理 | Admin 端：列表、删除、查看块数 | P1 |
| 召回排查 | `loggingRetriever` 自定义日志包装器，可观察召回数和相似度 | P1 |

### 11. AI 对话审计日志

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 自动捕获 | 每次 chat 请求由 `ChatTraceLoggingAdvisor` 自动写入，无需业务侧介入 | P0 |
| 完整内容记录 | 用户消息 / AI 回答 / 模型 / 用户 / 会话 / token 拆分 | P0 |
| 流式支持 | 流式响应也能完整捕获最终 reply 与 token usage | P0 |
| 错误记录 | 失败请求记 `status=error` + `error_message` | P0 |
| 写库容错 | 写库失败仅 `log.error`，**绝不影响 chat 响应** | P0 |
| Admin 列表查询 | 按 user_id / chat_id / status 筛选 + 分页 | P0 |
| Admin 详情查看 | 完整 prompt + response + token 拆分 + 复制 | P0 |
| 列表预览截断 | 列表里 user_message / response_text 截断到 100 字，避免响应体爆炸 | P1 |

### 12. 可观测性

| 需求 | 说明 | 优先级 |
|------|------|--------|
| Token 消耗监控 | 按模型 + 方向（input/output/total）累计与速率 | P0 |
| LLM 延迟分布 | 每次 LLM 调用 P50/P95/P99 延迟 | P0 |
| RAG 流水线分析 | 每个 advisor 的 P95 耗时（找瓶颈） | P1 |
| HTTP 接口指标 | QPS / P95 延迟 / 5xx 错误率 | P0 |
| 仪表盘可视化 | Grafana provisioning 自动加载，开箱即用 | P0 |
| 自动数据源 | Grafana 启动时自动配 Prometheus 数据源 | P0 |
| 模型筛选 | 仪表盘顶部模板变量，可单独看 deepseek / qwen-plus | P1 |

### 13. TTS 语音合成模块

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 音色管理 | 支持系统音色 + 自定义音色配置 | P1 |
| 声音复刻 | 支持用户上传语音样本复刻专属音色 | P2 |
| 音频缓存 | 卡片音频按 (card_id, voice_id) 缓存，避免重复合成 | P0 |
| 参数自定义 | 支持音量、语速、音调、指令参数调节 | P1 |
| 异步预生成 | 卡片组创建后异步批量生成音频 | P1 |
| Coze 插件支持 | 提供 X-API-Key 认证的独立 TTS 接口 | P1 |

### 14. API Key 管理模块

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 创建 Key | 用户自服务创建 API Key，返回完整明文（仅此一次） | P1 |
| 列表查询 | 查看自己的 Key 列表（仅显示前缀，不显示完整 Key） | P1 |
| 撤销 Key | 禁用指定 Key，禁止后续使用 | P1 |
| 安全存储 | 仅存储 key_hash（BCrypt），不存储明文 | P0 |
| 使用记录 | 记录最后使用时间，便于审计 | P1 |

---
## 五、状态体系（前后端共用）
### 1. 卡片组状态
| 状态 | 含义 | 可执行操作 |
|------|------|------------|
| 待学习 | 新建完成，未开始学习 | 开始学习 |
| 学习中 | 本轮未完成/存在需再学卡片 | 继续学习 |
| 学习完成 | 全卡已掌握，等待测试 | 进入测试 |
| 测试中 | 测试未全对，需重测整套 | 重学错题、重新测试 |
| 测试完成 | 测试全对，学习闭环结束 | 查看回顾 |

### 2. 卡片状态
| 状态 | 含义 |
|------|------|
| 未学习 | 未开始学习 |
| 学习中 | 知识点未学完 |
| 需再学 | 本轮学完需循环重学 |
| 已掌握 | 用户确认掌握 |
| 测试错题 | 本组测试中答错的卡片 |

### 3. 知识点状态
| 状态 | 含义 |
|------|------|
| 未展示 | 未展示给用户 |
| 已展示 | 已展示，待用户确认 |
| 已确认 | 用户点击确认，学习完成 |

### 4. 打卡池卡片状态
| 状态 | 含义 |
|------|------|
| 新学 | 首次出现在打卡池，未开始学习 |
| 学习中 | 已学习但未达到掌握标准 |
| 待复习 | 排期到期，等待下次复习 |
| 已掌握 | 连续多次"认识"标记，间隔拉长到阈值 |

### 5. API Key 状态
| 状态 | 含义 |
|------|------|
| 启用 | 正常可用 |
| 禁用 | 已撤销，无法使用 |

---
## 六、后台管理需求
| 需求 | 说明 | 优先级 |
|------|------|--------|
| 管理员登录 | 独立账号密码登录（BCrypt 加密） | P0 |
| 数据仪表盘 | 核心指标：用户数、卡片组总数、卡片总数、学习/测试通关率、打卡活跃度、趋势图 | P0 |
| 用户管理 | 用户列表（分页+搜索）、用户详情（学习数据、卡片组、错题记录、打卡统计） | P0 |
| 卡片组管理 | 查看/搜索/删除用户创建的卡片组，查看组内卡片、学习/测试状态 | P0 |
| 卡片管理 | 查看/搜索所有 AI 生成的卡片，查看学习/测试记录 | P1 |
| 词库管理 | 创建/编辑词库，手动添加单词，查看词库单词列表 | P1 |
| 单词内容管理 | 查看 word_contents，手动触发单个单词 LLM 刷新 | P1 |
| 打卡数据统计 | 每日打卡人数、打卡率、连续打卡天数分布、用户打卡趋势 | P1 |
| 错题数据统计 | 按卡片组/用户统计高频错题，辅助优化学习内容 | P1 |
| 知识库管理 | 上传文档（PDF/DOCX/PPTX/TXT/MD 等），自动分块向量化，查看/删除 | P1 |
| 音色管理 | 管理系统音色、导入官方音色库、测试合成、声音复刻 | P1 |
| AI 对话日志 | 查看所有用户的 AI 对话记录，支持筛选和详情查看 | P0 |
| API Key 管理 | 查看所有用户的 API Key，支持撤销操作 | P1 |
| 系统设置 | 预留系统配置管理入口 | P2 |

---
## 七、非功能性需求
| 需求 | 说明 |
|------|------|
| 微信小程序兼容 | 主要运行于微信小程序环境，兼容 H5 备用 |
| OCR 响应时间 | 图片识别应在 3 秒内返回结果 |
| LLM 生成时间 | 卡片生成应在 10 秒内完成（流式返回可优化体验） |
| TTS 可靠性 | DashScope CosyVoice 为主要 TTS 引擎，支持系统音色 + 声音复刻；百度 TTS 为降级链保留 |
| 数据安全 | 用户数据隔离，JWT 认证，密码 BCrypt 加密，API Key 仅存储 hash |
| 接口限流 | 对 LLM/OCR 等第三方 API 调用做频率控制 |
| 测试性能 | 单组测试加载、判分响应时间≤2秒 |
| 状态同步 | 学习/测试状态实时同步，无数据丢失 |
| Coze 插件集成 | 提供标准 REST API，支持 X-API-Key 认证，响应格式符合 Coze 插件规范 |

---

## 八、数据结构设计（核心变更）

### 设计原则

**单词结构化数据独立存储**：一个单词只有一条 `word_contents` 记录（LLM 生成），卡片组和打卡池都通过 `word_id` 引用。LLM 刷新内容后，所有引用方自动受益，避免数据冗余和不一致。

### 核心表结构

```
words                     ← 单词本体（唯一，一个单词一条记录）
  id, word_text, created_at

word_contents             ← 结构化知识点（LLM 生成，可独立刷新）
  id, word_id, pronunciation, pos, general_meaning,
  extended_meaning, example_sentence, memory_tip,
  llm_version, updated_at

cards                     ← 卡片（关联到卡片组 + 单词）
  id, group_id, word_id, sort_order, status, created_at

card_groups               ← 卡片组（保留现有结构，补充状态字段）
  id, user_id, title, group_status(待学习/学习中/学习完成/测试中/测试完成),
  source_text, source_image, created_at

knowledge_points          ← 知识点（每张卡片拆分为独立知识点）
  id, card_id, type(pronunciation/meaning/example/tip), content, 
  sort_order, status(未展示/已展示/已确认)

test_questions            ← 测试题目
  id, group_id, card_id, type(释义选择/单词选择/组词搭配/单词拼接),
  question_text, options(JSON), correct_answer, created_at

test_attempts             ← 答题记录
  id, question_id, user_id, user_answer, is_correct, created_at

error_book                ← 错题本（按卡片组归集）
  id, group_id, card_id, user_id, test_attempt_id, 
  resolved(是否已重学), created_at

word_bank                 ← 词库
  id, name, type(preset/user), created_by, created_at

word_bank_items           ← 词库中的单词
  id, bank_id, word_id, added_at

user_daily_pool           ← 用户打卡池（用户×词库的个性化进度）
  id, user_id, bank_id, word_id, 
  status(新学/学习中/待复习/已掌握),
  interval_days, next_review_at, last_review_at, 
  review_count, created_at

daily_checkin_log         ← 打卡日志
  id, user_id, bank_id, checkin_date, 
  new_words_count, review_words_count, 
  known_count, fuzzy_count, unknown_count

user_settings             ← 用户设置（含聊天偏好）
  id, user_id, daily_new_words(默认10), daily_review_words(默认20),
  checkin_reminder(默认false), reminder_time,
  chat_mode(默认chat), chat_model(默认deepseek), chat_stream(默认true),
  voice_id(音色偏好)

chat_conversations        ← AI 会话（用户-会话映射）
  id, user_id, chat_id, title, created_at

spring_ai_chat_memory     ← AI 对话消息（Spring AI 自动管理，大驼峰表名）
  conversation_id, content, type(USER/ASSISTANT/SYSTEM/TOOL), timestamp

vector_store              ← 知识库向量（PgVectorStore 自动管理）
  id, content, metadata(JSON), embedding(vector(1536))

chat_traces               ← AI 对话审计日志
  id, user_id, chat_id, model, user_message, response_text,
  prompt_tokens, completion_tokens, total_tokens,
  duration_ms, status, error_message, created_at

voices                    ← 音色配置
  id, name, provider, voice_code, tts_model, format,
  sample_rate, volume, speech_rate, pitch, instruction,
  is_default, is_active, created_at

word_audios               ← 单词音频缓存（v2 资源复用，word_id 替代 card_id）
  id, word_id, voice_id, audio_type, audio_url, duration_ms, file_size, created_at

card_kp_progress          ← 卡片知识点学习进度（v2 资源复用）
  id, card_id, kp_id, status(unshown/shown/confirmed)

test_session_questions    ← 测次关联（v2 资源复用，group_id + card_id → question_id）
  id, group_id, card_id, question_id, sort_order, created_at

random_test_pool          ← 随机测试池（v2 资源复用，review_count 计数）
  id, word_id, question_type, user_id, review_count, source, created_at, updated_at

documents                 ← 文档管理（Markdown 编辑/发布/向量化）
  id, user_id, title, content, category, tags, status(draft/published/archived),
  source_type, source_name, file_size, sort_order, knowledge_file_id, created_at, updated_at

agent_memories            ← 长期记忆
  id, user_id, memory_key, memory_value, created_at, updated_at

api_keys                  ← API Key
  id, user_id, name, key_hash, key_prefix, is_active, last_used_at, created_at

GraphThread               ← ReactAgent 会话线程
  thread_id, thread_name, is_released

GraphCheckpoint           ← ReactAgent 检查点
  checkpoint_id, parent_checkpoint_id, thread_id,
  node_id, next_node_id, state_data, state_content_type, saved_at
```

### 数据流向

```
         ┌──────────────────────────────────┐
         │          word_contents           │
         │   (LLM 生成，唯一，可刷新)         │
         └──────────┬───────────────────────┘
                    │ word_id
            ┌───────┴───────┐
            ▼               ▼
       ┌─────────┐    ┌──────────┐
       │  cards  │    │ 打卡池    │
       │(卡片组) │    │(每日复习) │
       └─────────┘    └──────────┘
            │               │
            ▼               ▼
       ┌─────────┐    ┌──────────┐
       │ 测试模块 │    │ 打卡记录  │
       └─────────┘    └──────────┘
            │
            ├──卡片组通关──→ 用户主动导入 → 打卡池
            │
            └──打卡勾选单词 → 创建/加入卡片组

ReactAgent 智能体：
  用户消息 → extractWords → checkExistingWords → recommendRelatedWords → createCardGroup
                   ↓
              卡片组创建成功
```

### 数据流向

```
         ┌──────────────────────────────────
         │          word_contents           │
         │   (LLM 生成，唯一，可刷新)         │
         └──────────┬───────────────────────┘
                    │ word_id
            ┌───────┴───────┐
            ▼               ▼
       ┌─────────┐    ┌──────────┐
       │  cards  │    │ 打卡池    │
       │(卡片组) │    │(每日复习) │
       └─────────┘    └──────────┘
            │               │
            ▼               ▼
       ┌─────────┐    ┌──────────┐
       │ 测试模块 │    │ 打卡记录  │
       └─────────┘    └──────────┘
            │
            ├──卡片组通关──→ 用户主动导入 → 打卡池
            │
            └──打卡勾选单词 → 创建/加入卡片组

ReactAgent 智能体：
  用户消息 → extractWords → checkExistingWords → recommendRelatedWords → createCardGroup
                   ↓
              卡片组创建成功
```

---

## 九、AI 聊天模块

### 模块概述

基于 Spring AI 1.1.7 + Spring AI Alibaba 1.1.2.0 的智能对话助手，支持流式 / 同步两种交互方式，具备**会话记忆**、**RAG 知识库增强**、**多 Advisor 流水线**、**ReactAgent 智能体**和**用户级数据隔离**。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 流式对话 | SSE 流式逐字输出，实时打字效果 | P0 |
| 同步对话 | 普通 POST 请求，一次性返回完整回复 | P1 |
| 多模型切换 | 支持 DeepSeek / 通义千问切换 | P0 |
| 会话记忆 | 基于 chatId 的对话历史，自动注入 LLM 上下文 | P0 |
| 会话管理 | 历史会话列表、切换会话、删除会话 | P1 |
| 知识库增强 | RAG 模式，基于上传文档回答问题，按用户隔离 | P1 |
| 多轮压缩 | 长对话自动压缩历史为独立查询，避免代词污染检索 | P1 |
| 查询重写 | 清洗冗余客套，精简成检索友好查询 | P1 |
| 多查询扩展 | 一查多变体，提升召回率 | P1 |
| 空召回拒答 | 检索 0 条时显式告知"无相关资料"，避免幻觉 | P0 |
| **ReactAgent 智能体** | **对话式创建卡片组，支持单词提取/查重/推荐/创建/撤销** | P0 |
| **长期记忆** | **保存/读取用户个性化信息，跨会话保留** | P1 |
| Admin 端入口 | 管理后台亦提供 AI 对话页面，与小程序端方案一致 | P1 |

### RAG 流水线

`LLMService.getRetrievalAugmentationAdvisor` 装配 5 段流水线：

```
用户消息
  ├─ ① CompressionQueryTransformer    （≥5 轮才生效，中文 prompt）
  │    把代词消歧成独立查询
  ├─ ② RewriteQueryTransformer        （每次都跑，中文 prompt）
  │    target=向量数据库；清洗冗余、保持中文
  ├─ ③ MultiQueryExpander             （3 个变体 + 原 query = 4 路并发）
  │    语义扩展，提升召回
  ├─ ④ VectorStoreDocumentRetriever   （按 user_id 过滤 + topK=10 + threshold=0.1）
  │    pgvector 检索，loggingRetriever 包一层打印召回数和分数
  └─ ⑤ ContextualQueryAugmenter       （allowEmptyContext=false）
       空召回时拒答，避免幻觉
       ↓
回答模型（DeepSeek 或 DashScope，按请求选）
```

**性能与成本约束：**

| 项 | 值 | 说明 |
|---|---|---|
| 单次请求 LLM 调用 | 3~4 次 | rewrite + expand 必跑；compression 多轮才跑；最终回答 1 次 |
| 单次请求向量检索 | 4 次 | 原 query + 3 个变体并发 |
| query 改写模型 | DeepSeek（固定） | 用便宜模型解耦最终回答 |
| 中文 prompt 强制 | 是 | 三个 .st 模板均强制保持原查询语言 |

详细参数（`LLMService` 顶部常量）：
- `RAG_SIMILARITY_THRESHOLD = 0.1`（pgvector + 中文 embedding 实测值）
- `RAG_TOP_K = 10`
- `COMPRESSION_MIN_ROUNDS = 5`
- `RAG_QUERY_VARIANTS = 3`

### ReactAgent 智能体工作流程

```
用户消息（可选携带 agent_context.candidate_words）
  ↓
ReactAgent（基于 agent-system.st 提示词）
  ├─ 已有候选词 → 跳过 extractWords，直接查重
  ├─ 无候选词 → 调 extractWords 提取单词
  ├─ 调 checkExistingWords 查重 → 告知用户已学/未学
  ├─ 可选：调 recommendRelatedWords 扩展词表
  └─ 用户确认后 → 调 createCardGroup 创建卡片组
       ↓
返回卡片组创建结果给用户
```

**工具集**：

| 工具 | 说明 |
|------|------|
| `extractWords` | 从文本提取英文单词 |
| `checkExistingWords` | 查询用户已学单词（已学/未学分类） |
| `recommendRelatedWords` | 基于种子词推荐相关词（1-10个） |
| `createCardGroup` | 创建卡片组（需用户明确确认） |
| `deleteLastCardGroup` | 撤销最近创建的卡片组 |
| `saveMemory` | 保存长期记忆（考试目标、英语水平等） |
| `recallMemory` | 读取长期记忆（不传 key 返回全部） |
| `deleteMemory` | 删除长期记忆 |

### 数据流

```
前端 → ChatController →
  解析 mode 参数：
    mode=chat（默认）→ LLMService（RAG 流水线）
    mode=agent → CardGroupAgentService（ReactAgent）
       ↓
  ChatTraceLoggingAdvisor（最外层，审计日志）
     ↓
  MessageChatMemoryAdvisor（加载历史消息）
     ↓
  RetrievalAugmentationAdvisor（RAG 5 段流水线，仅 chat 模式）
     ↓
  最终模型回答 → 流式/同步返回
     ↓
  Memory 持久化（Spring AI 自动）+ ChatTrace 持久化（审计）
```

### 会话记忆机制

| 组件 | 说明 |
|------|------|
| `MessageChatMemoryAdvisor` | Spring AI 框架 Advisor，自动管理对话上下文 |
| `MessageWindowChatMemory(maxMessages=20)` | 内存窗口，超过截断 |
| `JdbcChatMemoryRepository` | PostgreSQL 持久化（表 `SPRING_AI_CHAT_MEMORY`） |
| `snap_chat_conversations` | 自建业务表，绑定 user_id ↔ chat_id，存储会话标题 |
| `snap_agent_memories` | 长期记忆表，跨会话保留用户个性化信息 |

### API

| 端点 | 说明 |
|------|------|
| `POST /api/v1/chat/stream` | 流式对话（SSE，支持 mode=chat/agent） |
| `POST /api/v1/chat/call` | 同步对话（支持 mode=chat/agent） |
| `GET /api/v1/chat/conversations` | 当前用户会话列表 |
| `POST /api/v1/chat/conversations` | 新建会话 |
| `DELETE /api/v1/chat/conversations/{chatId}` | 删除会话（同清 memory） |
| `GET /api/v1/chat/messages/{chatId}` | 获取会话消息 |

**Agent 模式请求体扩展**：

```json
{
  "message": "帮我创建气候变化相关的卡片组",
  "model": "deepseek",
  "chat_id": "abc123",
  "mode": "agent",
  "agent_context": {
    "candidate_words": ["climate", "change", "temperature"],
    "ocr_text": "Climate change is affecting global temperatures..."
  }
}
```

### 前端页面

- 小程序端：`pages/chat/chat.vue`（气泡 UI + 会话 sheet + 模型切换 + Agent 模式）
- Admin 端：`admin/src/pages/chat/ChatPage.vue`（左侧会话列表 + 右侧对话，与小程序方案保持一致）

---

## 十、RAG 知识库模块

### 模块概述

管理员/用户上传文档到知识库，AI 对话时按 `user_id` 过滤检索相关片段注入上下文，提升回答准确性。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 文档上传 | 支持 PDF / DOCX / PPTX / TXT / MD / HTML / CSV / JSON | P1 |
| 自动分块 | Tika 解析 → TokenTextSplitter 分块 | P1 |
| MD 特殊处理 | Markdown 用 `MarkdownDocumentReader` 按标题分块 | P2 |
| 向量化存储 | DashScope text-embedding-v4（1536 维）→ pgvector | P1 |
| 用户级过滤 | 检索时按 metadata `user_id` 过滤，避免数据串户 | P0 |
| 对话增强 | RAG 流水线自动检索相似片段注入 prompt | P1 |
| 文档管理 | Admin 端：列表、删除、查看块数 | P1 |
| 召回排查 | `loggingRetriever` 自定义日志包装器，可观察召回数和相似度 | P1 |

### 技术架构

```
Admin/User 上传文档
  ├─ .md     → MarkdownDocumentReader（按标题分块）
  └─ 其他    → TikaDocumentParser → TokenTextSplitter
       ↓
  DashScope text-embedding-v4 向量化（1536 维）
       ↓
  PgVectorStore 存入 pgvector，metadata 含 user_id

用户 AI 对话
  RetrievalAugmentationAdvisor 自动经 5 段流水线检索 → 注入 prompt → LLM 回答
```

### 关键文件

| 文件 | 说明 |
|------|------|
| `controller/AdminKnowledgeController.java` | 上传/列表/删除 API |
| `service/LLMService.java` | RAG 流水线装配 + loggingRetriever |
| `config/ChatClientConfig.java` | PgVectorStore + ChatMemory 配置 |
| `prompts/query-compression.st` | 多轮压缩中文 prompt |
| `prompts/query-rewrite.st` | 查询重写中文 prompt |
| `prompts/query-expansion.st` | 多查询扩展中文 prompt |
| `admin/.../knowledge/KnowledgePage.vue` | 管理后台知识库页面 |

### 调参经验

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `similarityThreshold` | 0.1（中文）/ 0.5（英文） | 中文 embedding 偏严，0.5 常导致召回 0 条 |
| `topK` | 10 | 单次检索召回数 |
| `numberOfQueries` | 3 | 多查询扩展数（不含原 query） |

---

## 十一、AI 对话审计日志

### 模块概述

每次 AI 对话完成后，将完整请求 / 响应 / token 拆分 / 耗时 / 状态写入数据库，供管理员事后审查、排查问题、控制成本。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 自动捕获 | 每次 chat 请求由 `ChatTraceLoggingAdvisor` 自动写入，无需业务侧介入 | P0 |
| 完整内容记录 | 用户消息 / AI 回答 / 模型 / 用户 / 会话 / token 拆分 | P0 |
| 流式支持 | 流式响应也能完整捕获最终 reply 与 token usage | P0 |
| 错误记录 | 失败请求记 `status=error` + `error_message` | P0 |
| 写库容错 | 写库失败仅 `log.error`，**绝不影响 chat 响应** | P0 |
| Admin 列表查询 | 按 user_id / chat_id / status 筛选 + 分页 | P0 |
| Admin 详情查看 | 完整 prompt + response + token 拆分 + 复制 | P0 |
| 列表预览截断 | 列表里 user_message / response_text 截断到 100 字，避免响应体爆炸 | P1 |

### 数据结构

`snap_chat_traces` 表：

```
id                VARCHAR(36)   PRIMARY KEY
user_id           VARCHAR(36)   外键 → snap_users
chat_id           VARCHAR(100)  会话 ID
model             VARCHAR(50)   deepseek / dashscope / deepseek-agent / dashscope-agent
user_message      TEXT          用户输入
response_text     TEXT          AI 回答
prompt_tokens     INT
completion_tokens INT
total_tokens      INT
duration_ms       BIGINT
status            VARCHAR(20)   success / error
error_message     TEXT
created_at        TIMESTAMP
```

索引：`(user_id, created_at DESC)` / `(chat_id, created_at DESC)` / `(created_at DESC)`。

### 实现要点

`ChatTraceLoggingAdvisor` 实现 `CallAdvisor` + `StreamAdvisor`：
- `getOrder() = Ordered.HIGHEST_PRECEDENCE` —— 最外层 advisor，捕获最原始的用户消息和最终响应
- 流式路径用 `doOnNext` 累积所有 chunk，`doOnComplete` 触发写库
- 每次请求新建一份实例（携带 userId / chatId / model）

### API

| 路径 | 说明 |
|------|------|
| `GET /api/v1/admin/chat-traces?page=&pageSize=&userId=&chatId=&status=` | 分页列表 |
| `GET /api/v1/admin/chat-traces/{id}` | 详情（完整字段） |

### 与可观测性的关系

- **`snap_chat_traces`** 回答 "**这一次对话发生了什么**"（具体内容）
- **Prometheus metric** 回答 "**整体趋势怎么样**"（聚合指标）

两者互补，前者面向单次排错，后者面向健康度监控。

---

## 十二、可观测性

### 模块概述

通过 Spring AI 自带 Observation 埋点 + Micrometer + Prometheus + Grafana 实现 AI 对话与 HTTP 接口的全链路可观测，支持成本控制、性能分析、故障定位。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| Token 消耗监控 | 按模型 + 方向（input/output/total）累计与速率 | P0 |
| LLM 延迟分布 | 每次 LLM 调用 P50/P95/P99 延迟 | P0 |
| RAG 流水线分析 | 每个 advisor 的 P95 耗时（找瓶颈） | P1 |
| HTTP 接口指标 | QPS / P95 延迟 / 5xx 错误率 | P0 |
| 仪表盘可视化 | Grafana provisioning 自动加载，开箱即用 | P0 |
| 自动数据源 | Grafana 启动时自动配 Prometheus 数据源 | P0 |
| 模型筛选 | 仪表盘顶部模板变量，可单独看 deepseek / qwen-plus | P1 |

### 技术架构

```
backend-java
  Spring AI Observation（自动埋点）
    ↓
  Micrometer PrometheusMeterRegistry
    ↓
  /actuator/prometheus（暴露 Prometheus 文本格式）
    ↓
Prometheus（每 15s/30s 抓取，存 30 天）
    ↓
Grafana（PromQL 查询 + 仪表盘）
```

### 核心指标

| Metric | 含义 |
|--------|------|
| `gen_ai_client_token_usage_total` | 累计 token 消耗（含 `request_model` + `token_type` 标签） |
| `gen_ai_client_operation_seconds_*` | LLM 调用耗时直方图 |
| `spring_ai_advisor_seconds_*` | 各 advisor 耗时（如可用） |
| `http_server_requests_seconds_*` | HTTP 接口耗时 |

### Grafana 仪表盘

`SnapLearn AI Overview` 包含 10 个面板：
1. 累计 Input Token
2. 累计 Output Token
3. 累计调用次数
4. P95 延迟（最近 5 分钟）
5. Token 消耗速率（每分钟，按模型 + 方向）
6. 累计 Token 增长曲线
7. LLM 调用延迟分位（P50 / P95 / P99）
8. RAG 流水线 Advisor P95 耗时
9. HTTP 接口 QPS
10. HTTP 错误率（5xx 占比）

### 隐私安全

**Metric 不带任何用户 prompt / response 文本**——只记录 token 数和耗时。要查看具体对话内容，请使用 [第十一章](#十一ai-对话审计日志v22-新增) 的 `snap_chat_traces` 表。

`spring.ai.chat.observations.log-prompt` 配置项可控制是否把 prompt 写入 trace span，**生产环境保持关闭**。

### 部署形态

| 环境 | docker-compose | prometheus 配置 | target 写法 |
|------|----------------|-----------------|-------------|
| 本地（Windows + Docker Desktop） | `observability/docker-compose.yml` | `prometheus-dev.yml` | `host.docker.internal:8080` |
| 服务器（Linux） | `docker-compose.server.yml` | `prometheus.yml` | `snaplearn-backend:8080`（同 docker network） |

部署/重启脚本：
- 服务器：`./deploy-server.sh`（首次/有代码变更）+ `./restart-server.sh`（仅重启）
- 本地：`observability/restart.ps1`（Windows PowerShell）

### 端口

| 服务 | 端口 |
|------|------|
| Prometheus | 9090 |
| Grafana | 3100（容器内 3000） |

### 排查工作流

1. **Grafana 看趋势** —— 确认是否异常（token 飙升/延迟变大）
2. **`/actuator/prometheus`** —— 验证 backend 数据是否产生
3. **`/api/v1/targets`** —— 验证 Prometheus 抓取状态
4. **`snap_chat_traces` 表** —— 定位具体哪一次对话出了问题
5. **`loggingRetriever` 日志** —— 排查 RAG 召回质量

---

## 十三、TTS 语音合成模块

### 模块概述

基于阿里云 DashScope CosyVoice 的语音合成服务，支持多种音色、参数自定义、音频缓存和异步预生成。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 音色管理 | 支持系统预设音色 + 自定义音色配置 | P1 |
| 声音复刻 | 用户上传语音样本，复刻专属音色 | P2 |
| 音频缓存 | 按 (card_id, voice_id, audio_type) 缓存，避免重复合成 | P0 |
| 参数自定义 | 支持音量(0-100)、语速(0.5-2.0)、音调(0.5-2.0)、指令文本 | P1 |
| 异步预生成 | 卡片组创建后异步批量生成音频 | P1 |
| Coze 插件支持 | 提供 X-API-Key 认证的独立 TTS 接口，符合 Coze 插件规范 | P1 |
| 官方音色库 | 浏览和批量导入 DashScope 官方音色 | P1 |

### 技术架构

```
前端 / Coze 插件 → TtsController / CozeTtsController / AdminVoiceController
       ↓
  TtsService.synthesizeAndSave(voice, text, prefix, overrides?)
       ↓ 按 voice.provider 路由
  DashScopeCosyVoiceProvider.synthesize()
       ↓ POST SpeechSynthesizer API
  下载音频 → 写本地文件 → 返回相对 URL
```

**参数优先级**：接口传参 > Voice 实体值 > 系统默认值。

### 数据结构

`snap_voices` 表：

```
id              VARCHAR(36)    PRIMARY KEY
name            VARCHAR(100)   显示名称
provider        VARCHAR(50)    引擎（dashscope）
voice_code      VARCHAR(100)   音色标识
tts_model       VARCHAR(100)   合成模型（cosyvoice-v3-plus）
format          VARCHAR(10)    音频格式（mp3/pcm/wav/opus）
sample_rate     INTEGER        采样率（22050 默认）
volume          INTEGER        音量（0-100，默认50）
speech_rate     DOUBLE         语速（0.5-2.0，默认1.0）
pitch           DOUBLE         音调（0.5-2.0，默认1.0）
instruction     TEXT           指令文本（方言/情感/角色）
description     VARCHAR(500)   描述
is_default      BOOLEAN        是否系统默认
is_active       BOOLEAN        是否启用
created_at      TIMESTAMP
```

### Coze 插件接口

`POST /api/v1/coze/tts`（X-API-Key 认证）：

请求体：
```json
{
  "text": "Hello world",
  "voice": "longxiaochun_v3",
  "format": "mp3",
  "sample_rate": 24000,
  "volume": 80,
  "rate": 1.2,
  "pitch": 0.9
}
```

响应：
```json
{
  "audio_url": "https://example.com/uploads/audio/coze-abc123.mp3",
  "voice_code": "longxiaochun_v3"
}
```

---

## 十四、API Key 管理模块

### 模块概述

为外部系统（如 Coze 插件）提供无 JWT 的认证方式，用户自服务管理 API Key。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 创建 Key | 用户创建 API Key，返回完整明文（仅此一次） | P1 |
| 列表查询 | 查看自己的 Key 列表（仅显示前缀，不显示完整 Key） | P1 |
| 撤销 Key | 禁用指定 Key，禁止后续使用 | P1 |
| 安全存储 | 仅存储 key_hash（BCrypt），不存储明文 | P0 |
| 使用记录 | 记录最后使用时间，便于审计 | P1 |

### 数据结构

`snap_api_keys` 表：

```
id              VARCHAR(36)    PRIMARY KEY
user_id         VARCHAR(36)    外键 → snap_users
name            VARCHAR(100)   Key 名称
key_hash        VARCHAR(200)   BCrypt 哈希（唯一）
key_prefix      VARCHAR(10)    Key 前缀（前 8 位）
is_active       BOOLEAN        是否启用（默认 true）
last_used_at    TIMESTAMP      最后使用时间
created_at      TIMESTAMP
```

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/api-keys` | 创建 Key（返回完整明文，仅此一次） |
| GET | `/api/v1/api-keys` | 我的 Key 列表（不返回明文） |
| DELETE | `/api/v1/api-keys/{id}` | 撤销 Key |

### 安全设计

1. **哈希存储**：仅存储 `key_hash`（BCrypt），数据库泄露不会导致 Key 泄露
2. **一次可见**：创建时仅返回一次完整明文，后续无法找回
3. **前缀展示**：列表只显示前 8 位前缀，便于用户识别
4. **使用追踪**：记录 `last_used_at`，便于审计和清理过期 Key
5. **快速撤销**：支持即时禁用，无需等待 token 过期

---

## 十五、文档管理模块

### 模块概述

在管理端统一管理所有 MD 文档，支持在线编辑、批量导入、发布到向量库（RAG 知识库），并提供独立的 AI 对话入口。

### 功能清单

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 文档列表 | 表格展示，支持按标题搜索、分类/状态筛选、分页 | P0 |
| 在线编辑 | Markdown 编辑器，左侧编辑、右侧实时预览 | P0 |
| 批量导入 | 选择本地 .md 文件上传，文件名作为标题，内容写入 content | P1 |
| 发布到向量库 | 将 MD 内容向量化到 vector_store，关联 snap_knowledge_files | P0 |
| 撤销发布 | 删除向量库切片，保留文档原稿 | P0 |
| 删除文档 | 已发布先撤销，再删除 snap_documents 行 | P0 |
| 分类管理 | 支持自定义分类，按分类筛选 | P1 |
| 标签管理 | 逗号分隔标签，支持搜索 | P1 |
| 文档 AI 助手 | 独立 AI 对话入口，支持 RAG 检索 + Tool 调用（搜索/创建/修改文档） | P1 |

### 数据结构

`snap_documents` 表：见 [八、数据结构设计](#八数据结构设计核心变更)。

### 数据流

```
管理端编辑器 → snap_documents（Markdown 原稿，status=draft）
                   ↓ 发布
         KnowledgeVectorService 解析 → 切片 → 向量化
                   ↓
         snap_knowledge_files + vector_store
                   ↓
         snap_documents.status = 'published'
```

### 状态流转

```
draft → published → archived
  ↑         │
  └─────────┘ (撤销发布)
```

| status | 含义 |
|--------|------|
| draft | 草稿（可编辑） |
| published | 已发布（向量库已更新，修改需先撤销） |
| archived | 已归档（只读，不可编辑） |

### API

| 端点 | 说明 |
|------|------|
| `GET /api/v1/admin/documents` | 文档列表（搜索/分类/状态筛选） |
| `GET /api/v1/admin/documents/{id}` | 文档详情 |
| `POST /api/v1/admin/documents` | 创建文档 |
| `PUT /api/v1/admin/documents/{id}` | 更新文档 |
| `DELETE /api/v1/admin/documents/{id}` | 删除文档（已发布先撤销） |
| `POST /api/v1/admin/documents/{id}/publish` | 发布到向量库 |
| `POST /api/v1/admin/documents/{id}/unpublish` | 撤销发布 |
| `POST /api/v1/admin/documents/batch-publish` | 批量发布 |
| `POST /api/v1/admin/documents/import` | 批量导入 MD 文件 |
| `GET /api/v1/admin/documents/categories` | 分类列表 |

---

## 十六、资源复用（v2）+ 随机测试

### 模块概述

将知识点、测试题、语音等资源从"绑定卡片"改为"绑定单词"，实现跨卡片组复用，减少 LLM 调用和存储冗余。同时新增随机测试功能，基于复习计数实现间隔巩固。

### 核心改动

| 资源 | 改前 | 改后 |
|------|------|------|
| 知识点 (KP) | 绑定 card_id，存 content | 纯索引（word_id + point_type），不存内容，学习进度移到 snap_card_kp_progress |
| 测试题 | 绑定 group_id + card_id | 绑定 word_id（题库），独立 snap_test_session_questions（测次关联） |
| 语音 | snap_card_audios（card_id） | snap_word_audios（word_id），跨组复用 |

### 随机测试

| 需求 | 说明 | 优先级 |
|------|------|--------|
| 随机抽题 | 从 snap_random_test_pool 中按 review_count > 0 随机抽取 N 个单词 | P0 |
| 计数规则 | 首次生成入池 review_count=2；任何测试答错→4；随机测试答对→-1；减到 0 出库 | P0 |
| 实时标记 | 用户答错时前端立即调用 mark-wrong 接口，同步更新 review_count | P0 |
| 重新入库 | 已出库单词在任何测试中再次答错，review_count=4 重新入池 | P1 |

### 数据结构

新增表：

| 表 | 说明 |
|----|------|
| `snap_card_kp_progress` | 卡片知识点学习进度（card_id, kp_id, status） |
| `snap_word_audios` | 单词音频缓存（word_id, voice_id, audio_type） |
| `snap_test_session_questions` | 测次关联（group_id, card_id, question_id） |
| `snap_random_test_pool` | 随机测试池（word_id, user_id, review_count, source） |

### API（随机测试）

| 端点 | 说明 |
|------|------|
| `POST /api/v1/random-test/start` | 开始随机测试（参数 count，默认 10） |
| `POST /api/v1/random-test/submit` | 提交答案（question_ids + user_answers） |
| `POST /api/v1/random-test/mark-wrong` | 实时标记答错（word_id + question_type） |