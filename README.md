# mall 接口自动化测试实践

基于开源电商系统 [mall](https://github.com/macrozheng/mall) 的后台接口自动化测试工程。

本仓库包含两部分内容：

- **mall-1.0.3**：开源电商系统 `mall` 的源码（SpringBoot + MyBatis），作为被测系统。**源码版权归原作者 [macrozheng](https://github.com/macrozheng) 所有**，遵循 [Apache License 2.0](./LICENSE)，本仓库仅保留其原始代码并做少量本地配置调整（数据库密码改为环境变量）。
- **mall-api-test**：本人编写的后台接口自动化测试工程，基于 Java 21 + REST Assured + JUnit 5 + MySQL 驱动。

## 目录结构

```
mall-test
├── LICENSE                  # Apache License 2.0（针对 mall-1.0.3）
├── README.md
├── mall-1.0.3/              # 被测系统源码（第三方开源项目）
│   ├── mall-admin/          # 后台管理接口
│   ├── mall-common/
│   ├── mall-mbg/
│   ├── mall-security/
│   ├── mall-portal/
│   └── mall-search/
└── mall-api-test/           # 接口自动化测试工程
    └── src/test/java/com/mall/api/
        ├── UmsAdminControllerTest.java   # 用户认证
        ├── UmsAdminLoginApiTest.java
        ├── PmsProductControllerTest.java # 商品管理
        ├── UmsRoleControllerTest.java    # RBAC 角色
        └── UmsMenuControllerTest.java    # RBAC 菜单
```

## 测试覆盖

### 接口自动化（mall-api-test）

| 模块 | 覆盖内容 |
|---|---|
| 用户认证 | 登录成功/密码错误/账号禁用/用户不存在/无参；注册成功/用户名重复/空用户名/空格/空密码/非法邮箱；修改密码全流程；获取当前用户信息 |
| 商品管理 | 创建商品、按货号查询、商品详情、空参数异常场景 |
| RBAC 角色 | 创建、分页查询、修改、修改状态、删除 |
| RBAC 菜单 | 创建一级菜单、子菜单层级（level = 父 + 1）、详情、树形列表、删除 |

工程特点：

- `@BeforeAll` 统一登录获取 token，后续用例复用
- `@AfterEach` 通过 `DbUtil` 清理测试数据，用例之间互不影响
- 正向与异常场景兼顾，断言同时校验 HTTP 状态码、业务码、message 和 data
- 命名规范：单分支用 `xxxTest`，多分支用 `shouldXxxWhenYyy`

### 单元测试（mall-1.0.3 内，Mockito）

- `UmsAdminServiceImplTest`：18 个用例，覆盖修改密码、修改信息、注册、角色分配、登录、用户查询
- `UmsMenuServiceImplTest`：4 个用例，覆盖树形构建与菜单层级计算

## 测试过程中发现的缺陷

1. **商品创建接口缺少参数校验**：所有参数传 `null` 时，`insertSelective` 生成空列 SQL（`insert into pms_product () values ()`），直接抛出 `SQLSyntaxErrorException`，接口返回 500，而不是 400 参数错误。
2. **商品创建后列表查不到**：列表查询强制 `delete_status = 0`，而创建接口不传 `deleteStatus` 时写入 NULL，导致新建商品在后台列表中不可见。
3. **创建接口的失败分支不可达**：`PmsProductServiceImpl.create()` 中 `count` 被硬编码为 1，Controller 中 `count <= 0` 返回 `failed` 的分支永远不会执行。

## 运行测试

### 环境要求

- JDK 21
- Maven 3.8+
- MySQL 5.7+（导入 `mall-1.0.3/document/sql/mall.sql`）
- mall-admin 服务运行在 `http://localhost:8080`
- 数据库中存在可登录的测试账号 `test / 123456`

### 配置数据库密码

源码中的数据库密码已改为环境变量占位符，运行前需设置：

```bash
# Windows (PowerShell)
$env:DB_PASSWORD="你的MySQL密码"

# macOS / Linux
export DB_PASSWORD="你的MySQL密码"
```

### 执行测试

```bash
cd mall-api-test
mvn test
```

单独运行某个测试类：

```bash
mvn test -Dtest=UmsAdminControllerTest
```

## 许可证

`mall-1.0.3` 部分版权归 macrozheng 所有，遵循 [Apache License 2.0](./LICENSE)，原始仓库：<https://github.com/macrozheng/mall>。

`mall-api-test` 部分为本仓库作者编写。
