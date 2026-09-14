<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# data-pivot-plugin Changelog

## [Unreleased]

### Added

### Changed

## [2.3.0] - 2026-09-14
### Added
- Query、Analysis 对话框与 Settings 页的现代化交互（DialogWrapper、SearchTextField、空态/状态栏、主题高亮、快捷键、可访问名称）。
- Analysis 以字段值分布表替代 Lookup 弹层作为主界面，并支持复制 SQL / 选中值。
- 扩展 `unitTest`、`integrationTest`、`ideaUiTest` 覆盖查询/分析/设置/导航展示与纯逻辑。
- Gradle 任务 `verifyPluginFast`：只做插件结构校验，不下载额外 IDE。
- CI 对齐 IntelliJ Platform Plugin Template：Build → Test（`check`）→ Verify → Release draft；失败时上传测试报告。跨平台 `ideaUiTest` 矩阵改为手动/每周定时，PR 仍在 ubuntu 上跑完整测试套件。
- Query / Analysis / 装订线共用 `MappingResolver`：已配置 Settings 时走与 ORM 相同的策略路径；未配置时仅接受唯一命中，歧义不再暗中取最大相似度。
- Analysis 按 MySQL / PostgreSQL / Oracle / SQL Server 方言生成分布 SQL（含标识符引用），并限制前 20 个区分值。
- 查询与分析错误改为状态栏 + Notification（已注册 `Data Pivot Messages` 通知组），不再弹出阻塞式错误对话框。

### Changed
- Settings 的 Apply 状态改为真实 `isModified()` / `reset()`，工作副本与缓存分离。
- 动作文案、Query/Analysis 错误与装订线提示改为 i18n，并声明 `ActionUpdateThread.BGT`。
- 默认 HumpUnderline / JPA / MyBatis-Plus 回退的驼峰转换改为正确的捕获组大写（`hello_world_test` → `helloWorldTest`，`USER_ID` → `userId`）。
- 启动不再对每个数据源抢 JDBC 连接；连接池仅由 Query 按需创建，dispose 仍关闭 `QueryTool` 与驱动。
- Query / Analysis 的 JDBC 移出 EDT：先展示对话框与「正在查询」，再在后台任务回写结果。
- 装订线仅在高置信（精确名或 Settings 策略命中）时显示，并按 Profile 缩小数据源/库扫描；缓存随 PSI 修改、schema 刷新与 Settings Apply 失效，不再按访问次数驱逐。
- Settings 帮助文案改为如实描述映射优先级、模糊回退与 Analysis 方言 SQL；实体上的 `sqlCode` / 自定义 SQL 字段保留反序列化但不再生效。
- PostgreSQL / Oracle 在存在 schema 时用 `schema.table` 限定，不再把数据库名当成 schema。
- Oracle Analysis 给 `rs_count` / `percentage` 加引号，避免未加引号别名被转成大写后对不上结果列。
- Analysis 对 MongoDB / 未知 DBMS 提前给出用户可见错误，不再抛出未处理的 `IllegalArgumentException`。
- 多项目 Settings 下，映射与策略查找使用 PsiElement 所属 `Project`（`DataPivotApplication.getInstance(Project)`），不再依赖当前聚焦窗口。

## [2.2.0] - 2026-06-06
### Added
- 新增基于版本号的 GitHub Release 草稿流程，自动附加 `build/distributions` 中的插件 ZIP。
- 新增 GitHub Release 正式发布后自动推送 JetBrains Marketplace 的独立工作流。
- 恢复单元测试、集成测试和 UI 组件测试门禁，并提供手动跨平台 UI 组件测试。

### Changed
- 移除耗时且需要下载额外 IDE 的 Plugin Verifier 步骤。
- 测试任务禁用不兼容的 Gradle Configuration Cache，避免测试通过后因缓存序列化失败。

## [2.1.0] - 2026-06-05
### Changed
- 迁移到 IntelliJ Platform Gradle Plugin 2.x 最新模板结构，构建目标调整为 IntelliJ IDEA 2025.3+ / build 253+。
- 复用 IDEA Database Tools 数据源驱动，不再随插件打包 MySQL、PostgreSQL、Oracle、SQL Server、MongoDB 等数据库驱动。
- 明确数据库支持边界：当前维护 MySQL、PostgreSQL、Oracle、SQL Server 的导航和查询能力，不宣称覆盖 IDEA 支持的全部数据库方言。

### Added
- 增加单元测试、IntelliJ Platform 集成测试和设置页 UI 组件测试任务：`unitTest`、`integrationTest`、`ideaUiTest`。

## [2.0.0] - 2025-03-23
### Refactor
- 重构项目模块

## [1.1.2] - 2024-09-10

### Changed
- 关闭data-pivot mapping配置提醒

## [1.1.1] - 2024-09-07

### Changed
- 更新readme文件的数据库支持
- 关闭数据源检测提示


## [1.1.0] - 2024-07-18

### Added
- 便捷的装订线导航方式
- Data-Pivot Query 功能，支持便捷的数据查询
- Data-Pivot Query 功能支持远程搜索和本地搜索
- 提供默认映射策略，关联度匹配，匹配度为 0.9
- 提供关联度匹配映射的缓存功能
- 新增 data-pivot-test 模块
- 新增多数据源查询工具 QueryTool
- 优化 JDBC 连接池、防止 SQL 注入、支持中文搜索等

### Changed

- 更新操作说明 readme.md
- 更新 issue 模板
- 将 report 功能重命名为 Analysis
- Analysis 功能的映射配置采用关联度映射
- JDK 版本恢复为 JDK 11
- 更新包名



### Changed
- 关闭启动时数据连接检测
- 降低JDK版本
- 更改readme说明
- 先移除js解析引擎,插件瘦身

## [1.0.1] - 2024-07-04
### Changed
- 更新图标
- 更新插件ID
- 更新JDK版本

## [1.0.0] - 2024-02-24
### Added
- Project initialization, covering data analysis, ORM & ROM navigation.
- 项目初始化，包含数据分析、ORM&ROM导航。

[Unreleased]: https://github.com/weilhuang/data-pivot/compare/2.3.0...HEAD
[2.3.0]: https://github.com/weilhuang/data-pivot/compare/2.2.0...2.3.0
[2.2.0]: https://github.com/weilhuang/data-pivot/compare/2.1.0...2.2.0
[2.1.0]: https://github.com/weilhuang/data-pivot/compare/2.0.0...2.1.0
[2.0.0]: https://github.com/weilhuang/data-pivot/compare/1.1.2...2.0.0
[1.1.2]: https://github.com/weilhuang/data-pivot/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/weilhuang/data-pivot/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/weilhuang/data-pivot/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/weilhuang/data-pivot/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/weilhuang/data-pivot/commits/1.0.0
