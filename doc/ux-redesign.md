# data-pivot UX 重设计与测试策略

本文记录 Query、Analysis、Settings、导航相关界面的现状问题、重设计原则、交互前后对比、组件决策，以及测试金字塔与 CI 如何验证这些流程。

## 当前问题（代码与文档审计，而非预设根因）

对照 `src/main/java/com/data/pivot/plugin/view/`、`actions/`、`doc/image/*.gif` 与 i18n 资源后，主流程里实际存在这些问题：

### Query
- 使用原生 `JDialog`，而不是 IntelliJ `DialogWrapper`。
- 用灰色假 placeholder 文本模拟空态，焦点处理会把真实输入与「本地搜索 / 远程搜索」文案混淆。
- 高亮色写死 `Color(255, 255, 0)` 和 `JBColor.BLACK`，暗色主题下不可用。
- 空结果弹模态框后直接返回 `null`，查询失败时还会与 `QueryTool` 的错误框叠在一起。
- 复制行使用阻塞式 `Messages.showInfoMessage`；`java.util.Timer` 做防抖，而不是 `Alarm`。
- 标题、空数据、复制成功等文案硬编码中文，未走 `i18n`。
- 无状态栏、无加载态、无明确快捷键与无障碍名称。

### Analysis / Report
- 用 Code Completion `Lookup` 展示分布报表：`getLookupString()` 还有复制 SQL 的副作用。
- 类型文案为硬编码 `"count:"+…+" percentage:"+…+"%"`，无法浏览、排序或复制选中值。
- Lookup 适合插入补全，不适合只读报表。

### Settings
- `isModified()` 恒为 `true`，Apply 永远可点，Cancel/Reset 没有工作副本语义。
- 刷新是 `FlowLayout` 里的普通 `JButton`，映射对话框标签写死 `"module" / "choose"`。
- 无说明文字、无空态、无校验高亮；`createComponent()` 每次都会再初始化一遍。

### 导航
- `plugin.xml` 菜单分隔符小写英文；动作 `update` 未声明 `ActionUpdateThread`。
- Query / ORM 错误走 editor Hint，ROM 无 editor 时用 Dialog，口径不一致。
- 装订线 tooltip 硬编码英文。

## 重设计目标与原则

1. **跟 IDEA 走**：Java 代码库继续用 JB 组件、`FormBuilder`、`DialogWrapper`、`SearchTextField`、`ToolbarDecorator`、`ComboBox`、`ExtendableTextField`。未引入 Kotlin UI DSL，避免在全 Java 模块里混一套布局语言。
2. **行为兼容，交互升级**：远程/本地搜索语义、双击复制 JSON、多光标选列、Alt+Q/A/R/O 快捷键保持不变；空结果改为留在窗口里而不是关掉。
3. **状态可见**：空态、行数、搜索中、复制成功都在状态栏或 `JBTable.emptyText` 上，而不是再弹一层模态框。
4. **主题与 i18n**：`JBColor`、bundle keys（默认/`zh_CN`/`en_US`）。
5. **可测**：Query 通过 `QueryRunner` 注入，不连真实数据库；UI 测试只构造组件，不 `show()` 阻塞。
6. **可访问性**：搜索框 `accessibleName`、主操作有按钮与快捷键（复制、Esc 关闭、Ctrl/Cmd+F 本地、Ctrl/Cmd+Shift+F 远程）。

## 交互：Before → After

### Query
| Before | After |
| --- | --- |
| 非模态 `JDialog` + 两行假 placeholder | 非模态 `DialogWrapper` + 带 empty text 的 `SearchTextField` |
| 空结果：模态「查询数据为空」并放弃窗口 | 打开窗口，表格 empty text + 状态「没有可展示的行」 |
| 双击复制后 `Messages.showInfoMessage` | 状态栏「已复制为 JSON」；左侧 **Copy Row**；Ctrl/Cmd+C / Enter |
| 黄高亮、灰字 | 主题高亮；本地搜索只高亮、不打库 |
| 远程搜索无反馈 | 状态「正在查询数据库…」，随后更新行数 |

### Analysis
| Before | After |
| --- | --- |
| Lookup 列表，选中时复制 SQL | `AnalysisResultComponent`：Value / Count / Percentage 表 |
| 无法看完整 SQL | 只读 SQL 区 + **Copy SQL** |
| 空结果仍塞一条 Lookup | 表格空态 |

`DataPivotLookupElement` 仍保留为展示适配器：不再在 `getLookupString()` 里写剪贴板，供测试与将来补全场景使用。

### Settings
| Before | After |
| --- | --- |
| Apply 永远 dirty | 工作副本 vs 快照，`isModified()` / `apply()` / `reset()` |
| 顶部裸 Refresh 按钮 | `ToolbarDecorator` 额外 Refresh 动作 |
| 无说明 | 顶部说明 + 底部策略帮助 + 空表 empty text |
| 添加行对话框 GridBag + `choose` | `FormBuilder` + 包路径 `ExtendableTextField` + `ValidationInfo`；无数据源时禁用 database 下拉并显示提示 |

### 导航
| Before | After |
| --- | --- |
| 菜单文案写在 plugin.xml | `resource-bundle` 的 `action.*` / `group.*` keys |
| 分隔符 `query/analysis/navigation` | Title Case：Query / Analysis / Navigation |
| 硬编码中文 Hint | 统一 i18n；无 editor 时 Notification +「打开配置页」 |

快捷键保持：Alt+Q Query、Alt+A Analysis、Alt+R ORM、Alt+O ROM。

## 组件决策

| 组件 | 选择 | 原因 |
| --- | --- | --- |
| 查询/分析窗口 | `DialogWrapper`，`setModal(false)` | 平台标准；保留「不挡住编辑器」 |
| 搜索 | `SearchTextField` | empty text、历史、无假 placeholder |
| 表单 | `FormBuilder` | Java 侧对应 UI DSL 的表单间距 |
| 设置表 | 已有 `ToolbarDecorator` + extra `AnAction` | 与 Settings 页工具条一致 |
| 包选择 | `ExtendableTextField` | 比旁路 `JButton("choose")` 更接近 IDEA 字段 |
| 防抖 | `Alarm` | 绑定 `Disposable`，测试走同步 `perform*Search`，避开 Timer 线程 |
| 复制 | `CopyPasteManager` | 平台剪贴板，测试断言 `lastCopiedText` |
| Kotlin UI DSL | **不采用** | 工程是 Java；JB + FormBuilder 已覆盖 253+ 惯例 |

`QueryRunner` 是 Query UI 与 `QueryTool` 之间的缝：生产用 `QueryTool::query`，测试注入内存数据。

## 测试金字塔

```
ideaUiTest  (*UiTest)     对话框/设置页/表格/动作 enablement
        ▲
integrationTest           启动、策略、plugin.xml 契约
        ▲
unitTest                  SQL、引用、映射快照、分析行解析、方言
```

### Gradle 任务

| 任务 | 层 | 包含 |
| --- | --- | --- |
| `unitTest` | 单元 | 排除 `*IntegrationTest` 与 `*UiTest` 的其余测试 |
| `integrationTest` | 平台逻辑 / 轻量 e2e | `*IntegrationTest` |
| `ideaUiTest` | UI 组件 | `*UiTest`，构造组件，不连真实 DB |
| `check` | 门禁 | 上面三个 |
| `verifyPluginFast` | 打包结构 | `verifyPluginStructure`，**不**跑 Plugin Verifier |
| `buildPlugin` | 产物 | Marketplace ZIP |

`test`（默认）同样排除 UI/集成，供 IDE 快速运行。

### UI 测试如何映射到流程

| 流程 | 测试类 | 关键断言 |
| --- | --- | --- |
| Query 打开/状态/搜索框 | `QueryTableComponentUiTest` | 行数、列、状态文案、accessible name、标题含表名 |
| Query 本地搜索 | 同上 | 高亮单元格数；注入 runner 调用次数为 0 |
| Query 远程搜索 | 同上 | stub runner 按 `likeValue` 过滤；表格与状态更新 |
| Query 空态 | 同上 | empty text + status empty |
| Query 复制 | 同上 | JSON 含字段；无选择时状态提示 |
| Analysis 报表 | `AnalysisResultUiTest` | 三列、SQL 只读、空态、复制 SQL/值 |
| Analysis Lookup 适配 | 同上 | insert 文本不是 SQL；展示 value + info |
| Settings | `DataPivotSettingsUiTest` | 说明/帮助、初始未修改、reset、表单标签、无库提示 |
| 映射表 | `DataPivotTableViewUiTest` | 列值、reload、empty text |
| 导航动作 | `DataPivotActionPresentationUiTest` | 无 PsiField / DbColumn 时 disabled |
| 纯逻辑 | `QueryToolTest`、`DataPivotUtilTest`、`MappingSettingSupportTest`、`AnalysisResultModelTest`、`DBTypeTest`、`StringConverterTest` | SQL、引用、快照、分析行、方言 |
| 插件契约 | `DataPivotStartupIntegrationTest` | 依赖、startup、configurable、动作 id 与快捷键、resource-bundle |

UI 测试禁止：`Thread.sleep`、真实 JDBC、`dialog.show()` 阻塞。远程搜索测试调用 `performRemoteSearch`，绕过 300ms `Alarm`。

## CI 流水线

参考 [IntelliJ Platform Plugin Template `build.yml`](https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/.github/workflows/build.yml)。

PR 与 `main` 上的 **Build** workflow：

1. **Build**：`./gradlew buildPlugin`，上传 `build/distributions/*.zip`。
2. **Test**（依赖 Build）：`./gradlew check --continue --no-configuration-cache`。失败上传 `build/reports/tests`。
3. **Verify plugin**（依赖 Build）：`./gradlew verifyPluginFast`。
4. **Release draft**（仅非 PR，且版本检测为新版本）：复用本仓库已有的按版本号刷新 draft 逻辑，而不是模板里「删掉所有 draft」。产物来自 Build artifact，不再编一次插件。

其它对齐点：`free-disk-space`、Java 21 Zulu、`gradle/actions/setup-gradle@v6`、`actions/checkout@v6`、`upload-artifact@v7`。PR 上 `cancel-in-progress: true`；`main` 上为 false，避免打断 draft 发布。

### 为何不用模板里的完整 `verifyPlugin` / Plugin Verifier

2.2.0 已去掉「耗时且需要下载额外 IDE 的 Plugin Verifier」。模板的 `verifyPlugin` 在 IntelliJ Platform Gradle Plugin 2.x 下仍可能拉起 `verifyPluginCompatibility`（按 `pluginVerification { ides { recommended() } }` 下额外 IDE）。

本仓库用 `verifyPluginFast` → `verifyPluginStructure`：检查 plugin.xml / 打包结构，不下载别的 IDE。`runPluginVerifier` 仍可通过 `.run/Run Verifications.run.xml` 本地执行。

### 跨平台 UI 矩阵

`.github/workflows/run-ui-tests.yml`：**不**作为 PR 门禁。触发：

- `workflow_dispatch`（手动）
- 每周一 03:00 UTC `schedule`

矩阵：ubuntu / windows / macos，只跑 `ideaUiTest`。PR 完整套件已经在 ubuntu 的 Test job 里。

## 如何本地验证

```bash
./gradlew unitTest
./gradlew integrationTest
./gradlew ideaUiTest
./gradlew check
./gradlew verifyPluginFast
```

手动：在 2025.3+ 中 `Run Plugin`，对实体字段 Alt+Q / Alt+A，Settings → Tools → Data-Pivot Configuration 增删映射，确认 Apply 仅在修改后可用。
