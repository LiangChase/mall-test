# mall 项目测试开发指南

> 适用环境：JDK 21 + Maven 3.9 + Spring Boot 2.7.5  
> 目标：为项目建立完整的测试体系，作为测开方向的项目经历

---

## 目录

- [1. 快速开始](#1-快速开始)
- [2. 项目模块总览](#2-项目模块总览)
- [3. 测试分层策略](#3-测试分层策略)
- [4. 写第一个单元测试](#4-写第一个单元测试)
- [5. 各模块测试清单](#5-各模块测试清单)
- [6. 测试模式速查](#6-测试模式速查)
- [7. 特殊场景处理](#7-特殊场景处理)
- [8. 覆盖率配置](#8-覆盖率配置)
- [9. 常见问题](#9-常见问题)

---

## 1. 快速开始

### 1.1 环境确认

```bash
java -version   # JDK 21
mvn -version    # 3.9+
```

### 1.2 运行测试

```bash
# 运行全部测试
mvn test

# 运行单个模块的测试
mvn test -pl mall-common

# 运行单个测试类
mvn test -pl mall-common -Dtest=CommonResultTest

# 跳过测试（仅编译）
mvn test -DskipTests=true
```

### 1.3 项目已完成的配置修改

父 `pom.xml` 中 Lombok 已升级为 `1.18.34`（兼容 JDK 21）。测试时注意：
- 项目原来的 `<skipTests>true</skipTests>` 在父 POM 的 `<properties>` 中，命令行 `mvn test` 默认会覆盖它（`maven-surefire-plugin` 的 `skipTests`
  默认 false），但如果你发现测试没执行，显式加上 `-DskipTests=false` 即可

---

## 2. 项目模块总览

```
mall（父）
├── mall-common     15 个类 | 工具类、通用 API 封装、Redis 服务
├── mall-mbg       230 个类 | MyBatis Generator 自动生成的 model + mapper
├── mall-security   15 个类 | Spring Security + JWT 认证鉴权
├── mall-admin     153 个类 | 后台管理（31 个 Service + ~30 个 Controller）
├── mall-portal     84 个类 | 前台商城（15 个 Service + 13 个 Controller）
├── mall-search     11 个类 | Elasticsearch 商品搜索
└── mall-demo       13 个类 | 演示/示例模块
```

**依赖关系**：

```
mall-common ← mall-mbg ← mall-demo
mall-common ← mall-security ← mall-admin
mall-common ← mall-security ← mall-portal
mall-mbg ← mall-search
```

---

## 3. 测试分层策略

采用经典测试金字塔，**由下往上**逐层写：

```
        ┌─────────────────┐
        │  集成测试 (10%)   │  @SpringBootTest + H2 + Mock 外部服务
        ├─────────────────┤
        │  Web 层 (20%)    │  @WebMvcTest + MockMvc + MockBean
        ├─────────────────┤
        │  Service 层 (30%)│  @ExtendWith(MockitoExtension) + @Mock + @InjectMocks
        ├─────────────────┤
        │  纯单元测试 (40%) │  纯 Java，不启动 Spring 容器
        └─────────────────┘
```

| 层 | 测试对象 | 工具 | 编写优先级 |
|---|---------|------|:---:|
| 纯单元 | 工具类、POJO、枚举、静态方法 | JUnit 5 + AssertJ | ⭐⭐⭐ 先写 |
| Service 层 | Service 实现类，Mock Mapper/DAO | Mockito + @InjectMocks | ⭐⭐⭐ 先写 |
| Web 层 | Controller 接口行为 | MockMvc + @WebMvcTest | ⭐⭐ 次写 |
| 集成测试 | DAO XML SQL、完整 Service→DB | @SpringBootTest + H2 + @Sql | ⭐ 最后补 |

---

## 4. 写第一个单元测试

### 4.1 最简单的例子 — CommonResultTest

```java
// mall-common/src/test/java/com/macro/mall/common/api/CommonResultTest.java
package com.macro.mall.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonResultTest {

    @Test
    @DisplayName("success() 应该返回 code=200")
    void shouldReturnSuccessCode() {
        CommonResult<String> result = CommonResult.success("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("success(null) 不应该抛异常")
    void shouldHandleNullData() {
        CommonResult<Object> result = CommonResult.success(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("failed(String) 应该返回自定义消息")
    void shouldReturnCustomFailMessage() {
        CommonResult<?> result = CommonResult.failed("自定义错误");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("自定义错误");
    }

    @Test
    @DisplayName("validateFailed() 应该返回 code=404")
    void shouldReturnValidateFailed() {
        CommonResult<?> result = CommonResult.validateFailed("参数不合法");

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo("参数不合法");
    }

    @Test
    @DisplayName("unauthorized() 应该返回 code=401")
    void shouldReturnUnauthorized() {
        CommonResult<String> result = CommonResult.unauthorized("请先登录");

        assertThat(result.getCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("forbidden() 应该返回 code=403")
    void shouldReturnForbidden() {
        CommonResult<String> result = CommonResult.forbidden("无权限");

        assertThat(result.getCode()).isEqualTo(403);
    }
}
```

### 4.2 运行验证

```bash
cd D:/Learn/projects/mall-1.0.3
mvn test -pl mall-common -Dtest=CommonResultTest
```

---

## 5. 各模块测试清单

### 5.1 mall-common（纯单元测试，0 外部依赖）⭐ 第一优先级

**目标覆盖率：85%+**

| 测试类 | 被测对象 | 难度 | 测试数 |
|--------|---------|:--:|:-----:|
| `CommonResultTest` | 所有工厂方法（success/failed/validateFailed/unauthorized/forbidden） | 低 | 8+ |
| `CommonPageTest` | restPage(List)、restPage(Page)、边界（空列表） | 低 | 5+ |
| `ResultCodeTest` | 枚举值映射（SUCCESS=200, FAILED=500 等） | 低 | 5+ |
| `AssertsTest` | fail(String) 和 fail(IErrorCode) 都抛 ApiException | 低 | 3+ |
| `ApiExceptionTest` | 构造器、getErrorCode()、getMessage() | 低 | 4+ |
| `RequestUtilTest` | getRequestIp：x-forwarded-for、多级代理、直连、localhost | 中 | 6+ |
| `RedisServiceImplTest` | Mock RedisTemplate，覆盖 String/Hash/Set/List 全部方法 | 中 | 15+ |
| `WebLogAspectTest` | Mock JoinPoint + ProceedingJoinPoint，验证 AOP 不抛异常 | 中 | 3+ |

**测试目录**：`mall-common/src/test/java/com/macro/mall/common/`

### 5.2 mall-security（纯单元 + Mock）⭐ 第一优先级

**目标覆盖率：90%+**（安全模块，核心中的核心）

| 测试类 | 被测对象 | 技巧 | 测试数 |
|--------|---------|------|:-----:|
| `JwtTokenUtilTest` | 完整 token 生命周期：生成 → 解析 → 校验 → 刷新 | ReflectionTestUtils 注入 secret/expiration/tokenHead | 10+ |
| `DynamicAccessDecisionManagerTest` | decide()：空 configAttributes → 放行；匹配 role → 放行；不匹配 → 抛异常 | Mock Authentication + ConfigAttribute | 5+ |
| `DynamicSecurityMetadataSourceTest` | getAttributes()：精确匹配 / 通配符匹配 / 无匹配 | Stub DynamicSecurityService | 5+ |
| `JwtAuthenticationTokenFilterTest` | 有效 token 设 SecurityContext / 无效跳过 / 无 header 跳过 | Mock FilterChain + HttpServletRequest/Response | 6+ |
| `RestAuthenticationEntryPointTest` | commence → 写 JSON 401 | MockHttpServletResponse | 2+ |
| `RestfulAccessDeniedHandlerTest` | handle → 写 JSON 403 | MockHttpServletResponse | 2+ |
| `RedisCacheAspectTest` | @CacheException 方法异常 → 抛回 / 无注解异常 → 吞掉 | @SpringBootTest + @EnableAspectJAutoProxy | 3+ |
| `DynamicSecurityFilterTest` | 白名单放行 / OPTIONS 放行 / 需认证走 beforeInvocation | Mock FilterInvocation | 4+ |
| `SpringUtilTest` | setApplicationContext → getBean | Mock ApplicationContext | 2+ |
| `IgnoreUrlsConfigTest` | @ConfigurationProperties 绑定 urls 列表 | Spring 属性绑定测试 | 2+ |

**测试目录**：`mall-security/src/test/java/com/macro/mall/security/`

### 5.3 mall-admin（Service Mock 单元测试）⭐ 第二优先级

**目标覆盖率：Service 层 70%+**

每个 Service 一个测试类，模式固定：

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceImplTest {

    @Mock
    private XxxMapper xxxMapper;        // MBG 生成的 Mapper

    @Mock
    private XxxDao xxxDao;              // 自定义 DAO

    @InjectMocks
    private XxxServiceImpl xxxService;  // 被测对象

    // 测试方法...
}
```

#### 重点 Service（必须覆盖）

| Service | 核心方法（重点测） | 注入数 | 复杂度 |
|---------|-------------------|:-----:|:-----:|
| `UmsAdminServiceImplTest` | login（正常/密码错误/账号禁用）、register（成功/重复）、updatePassword（-1/-2/-3/1）、getAdminByUsername（缓存/DB）、refreshToken、loadUserByUsername | 6 | 高 |
| `PmsProductServiceImplTest` | create（完整关联数据）、update（SKU 差异）、list（多条件组合）、updateVerifyStatus、updateDeleteStatus | 13 | 最高 |
| `OmsOrderServiceImplTest` | delivery（批量发货+操作历史）、close、delete、updateReceiverInfo、updateMoneyInfo | 5 | 高 |
| `UmsRoleServiceImplTest` | allocMenu（删旧插新）、allocResource、getMenuList | 6 | 中 |
| `PmsBrandServiceImplTest` | createBrand、updateBrand（连带更新 product.brandName）、deleteBrand | 3 | 中 |
| `SmsCouponServiceImplTest` | create（全店/指定分类/指定商品）、delete（级联删关联） | 6 | 中 |

#### 其余 Service（按统一模式覆盖）

| Service | 方法数 | 测试要点 |
|---------|:----:|---------|
| `CmsPrefrenceAreaServiceImpl` | 1 | listAll |
| `CmsSubjectServiceImpl` | 2 | listAll、list（PageHelper 分页） |
| `OmsCompanyAddressServiceImpl` | 1 | list |
| `OmsOrderReturnApplyServiceImpl` | 4 | list、delete（仅 status=3 可删）、updateStatus（各状态分支）、getItem |
| `OmsOrderReturnReasonServiceImpl` | 6 | CRUD + updateStatus + list 分页 |
| `OmsOrderSettingServiceImpl` | 2 | getItem、update |
| `PmsProductAttributeCategoryServiceImpl` | 6 | create/update/delete/getItem/getList/getListWithAttr |
| `PmsProductAttributeServiceImpl` | 6 | create/update/delete/getList/getItem/getProductAttrInfo |
| `PmsProductCategoryServiceImpl` | 8 | create/update/delete（子分类不能删）/listWithChildren/navStatus/showStatus |
| `PmsSkuStockServiceImpl` | 2 | getList、update（replace 逻辑） |
| `SmsCouponHistoryServiceImpl` | 1 | list 分页 |
| `SmsFlashPromotionServiceImpl` | 6 | CRUD + updateStatus + list |
| `SmsFlashPromotionProductRelationServiceImpl` | 6 | CRUD + getList + getCount |
| `SmsFlashPromotionSessionServiceImpl` | 7 | CRUD + updateStatus + selectList（含 relationService.getCount） |
| `SmsHomeAdvertiseServiceImpl` | 6 | CRUD + updateStatus + list（含时间过滤） |
| `SmsHomeBrandServiceImpl` | 5 | create 批量/updateSort/delete/updateRecommendStatus/list |
| `SmsHomeNewProductServiceImpl` | 5 | 同上模式 |
| `SmsHomeRecommendProductServiceImpl` | 5 | 同上模式 |
| `SmsHomeRecommendSubjectServiceImpl` | 5 | 同上模式 |
| `UmsAdminCacheServiceImpl` | 8 | 全部 Redis key 拼接 + 缓存读写的正确性 |
| `UmsMemberLevelServiceImpl` | 1 | list(defaultStatus) |
| `UmsMenuServiceImpl` | 7 | CRUD + treeList（递归树构造）/updateHidden |
| `UmsResourceCategoryServiceImpl` | 4 | CRUD |
| `UmsResourceServiceImpl` | 6 | CRUD + 每次增删改后调用 cacheService.delResourceListByResource |

### 5.4 mall-portal（Service Mock 单元测试）⭐ 第二优先级

| Service | 核心方法 | 特殊处理 |
|---------|---------|---------|
| `UmsMemberServiceImplTest` | register（验证码校验→唯一性检查→设默认等级→BCrypt）、login、refreshToken、generateAuthCode（6位随机数）、updatePassword、getCurrentMember | Mock `SecurityContextHolder` 策略上下文或直接用 `MemberDetails` |
| `OmsPortalOrderServiceImplTest` | generateOrder（完整下单链：地址校验→优惠券计算→积分抵扣→库存检查→锁库存→生成订单号→清购物车→发延迟消息）、cancelOrder、paySuccess、cancelTimeOutOrder | **最复杂的 Service**（~770 行，14 个注入），分多个测试场景 |
| `OmsCartItemServiceImplTest` | add（新增 vs 累加数量）、list、listPromotion、updateQuantity、delete、clear | Mock `PortalProductDao` |
| `OmsPromotionServiceImplTest` | calcCartPromotion：promotionType=1 单品/3 阶梯折扣/4 满减/其他 无折扣 | 核心计算逻辑，纯数据驱动测试 |
| `UmsMemberCouponServiceImplTest` | add（数量检查/时间检查/每人限领）、listHistory、listCart（可用/不可用）、listByProduct | |
| `UmsMemberReceiveAddressServiceImplTest` | add/delete/update（换默认地址时的旧地址清除）/list/getItem | |
| `PmsPortalProductServiceImplTest` | search（5种排序方式）、categoryTreeList（递归）、detail（组装7个子查询） | |
| 其余 7 个 Service | 按模式覆盖 | |

### 5.5 mall-admin / mall-portal Web 层（MockMvc）⭐ 第三优先级

**模式**：

```java
@WebMvcTest(UmsAdminController.class)      // 只加载指定 Controller
class UmsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean                               // Mock 掉 Controller 依赖的 Service
    private UmsAdminService adminService;

    @Test
    @DisplayName("POST /admin/login 成功返回 token")
    void shouldReturnTokenWhenLoginSuccess() throws Exception {
        // given
        when(adminService.login("admin", "123456")).thenReturn("mock-token");

        // when & then
        mockMvc.perform(post("/admin/login")
                .param("username", "admin")
                .param("password", "123456")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").value("mock-token"))
            .andExpect(jsonPath("$.data.tokenHead").value("Bearer "));
    }

    @Test
    @DisplayName("POST /admin/login 密码错误返回 validateFailed")
    void shouldReturnFailedWhenBadPassword() throws Exception {
        when(adminService.login("admin", "wrong")).thenReturn(null);

        mockMvc.perform(post("/admin/login")
                .param("username", "admin")
                .param("password", "wrong"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}
```

**建议覆盖的 Controller**：

**mall-admin（选 10 个重点）**：
- `UmsAdminControllerTest`（login / register / refreshToken / updatePassword / info / logout）
- `PmsProductControllerTest`（create / update / list / 各种状态更新）
- `OmsOrderControllerTest`（delivery / close / delete）
- `UmsRoleControllerTest`（allocMenu / allocResource）
- 其余按需覆盖

**mall-portal（选 8 个重点）**：
- `UmsMemberControllerTest`（register / login / refreshToken）
- `OmsPortalOrderControllerTest`（generateOrder / cancelOrder / list）
- `OmsCartItemControllerTest`（add / list / updateQuantity / delete）

### 5.6 mall-search（Mock 单元测试）⭐ 第三优先级

| 测试类 | 内容 |
|--------|------|
| `EsProductServiceImplTest` | Mock `EsProductDao` + `EsProductRepository` + `ElasticsearchRestTemplate`，测 importAll/create/delete/search/simpleSearch/recommend/searchRelatedInfo |
| `EsProductControllerTest` | @WebMvcTest，Mock EsProductService |

### 5.7 mall-demo

- `FlagValidatorClassTest`：`isValid(null)` → true（null 值放行），`isValid(1, [1,2,3])` → true，`isValid(4, [1,2,3])` → false

### 5.8 集成测试（最后补）

用到 MyBatis XML 中的复杂 SQL 时，需要集成测试：

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // 用 H2 替换 MySQL
@Sql(scripts = "/test-data.sql")   // 预置测试数据
class PmsProductDaoTest {

    @Autowired
    private PmsProductDao productDao;

    @Test
    void shouldGetUpdateInfo() {
        PmsProductResult result = productDao.getUpdateInfo(1L);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("测试商品");
    }
}
```

**需要添加**：`mall-admin/src/test/resources/application-test.yml`
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:mall_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.mongo.MongoAutoConfiguration
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
      - org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
```

---

## 6. 测试模式速查

### 6.1 纯单元测试（不启动 Spring）

```java
// 不需要任何 @ExtendWith，最轻量
class MyUtilTest {
    @Test
    void testSomething() {
        String result = MyUtil.doSomething("input");
        assertThat(result).isEqualTo("expected");
    }
}
```

### 6.2 Service 层测试（Mockito）

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceImplTest {

    @Mock
    private XxxMapper mockMapper;

    @Mock
    private XxxDao mockDao;

    @InjectMocks
    private XxxServiceImpl service;

    @Test
    void testCreate() {
        // given - 准备 Mock 数据
        XxxEntity entity = new XxxEntity();
        when(mockMapper.insertSelective(any())).thenReturn(1);

        // when - 执行被测方法
        int result = service.create(xxxParam);

        // then - 验证
        assertThat(result).isEqualTo(1);
        verify(mockMapper).insertSelective(any());  // 验证调用
    }

    @Test
    void testCreateWhenNullInput() {
        // given
        when(mockMapper.insertSelective(null)).thenThrow(RuntimeException.class);

        // when & then
        assertThatThrownBy(() -> service.create(null))
            .isInstanceOf(RuntimeException.class);
    }
}
```

### 6.3 Web 层测试（MockMvc）

```java
@WebMvcTest(XxxController.class)
class XxxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XxxService mockService;

    @Test
    void getListShouldReturnPage() throws Exception {
        // given
        List<XxxDto> mockList = Arrays.asList(new XxxDto(), new XxxDto());
        when(mockService.list(anyInt(), anyInt())).thenReturn(mockList);

        // when & then
        mockMvc.perform(get("/xxx/list")
                .param("pageNum", "1")
                .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(2));
    }
}
```

### 6.4 参数化测试（数据驱动）

适合 `JwtTokenUtil`、`RequestUtil`、`OmsPromotionService.calcCartPromotion` 这类"入参→出参"逻辑：

```java
@ParameterizedTest
@CsvSource({
    "1, SUCCESS",
    "2, FAILED",
    "3, PENDING"
})
void shouldReturnCorrectStatus(int input, String expected) {
    assertThat(service.getStatus(input)).isEqualTo(expected);
}
```

```java
@ParameterizedTest
@MethodSource("provideGetRequestIpParams")
void testGetRequestIp(String headerValue, String expectedIp) {
    // ...
}
static Stream<Arguments> provideGetRequestIpParams() {
    return Stream.of(
        Arguments.of("192.168.1.1", "192.168.1.1"),
        Arguments.of("192.168.1.1, 10.0.0.1", "192.168.1.1"),
        Arguments.of(null, "127.0.0.1")
    );
}
```

### 6.5 用 AssertJ 写可读断言

```java
// 不用 JUnit 原生的 assertEquals（参数顺序容易搞反）
assertEquals(expected, actual);                  // ❌ 可读性差

// 用 AssertJ 的链式断言（失败信息自动生成）
assertThat(actual).isEqualTo(expected);           // ✅ 语义清晰
assertThat(list).hasSize(5).contains(item);
assertThat(result.getCode()).isEqualTo(200);
assertThatThrownBy(() -> service.doStuff(null))   // ✅ 断言异常
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("不能为空");
```

---

## 7. 特殊场景处理

### 7.1 如何 Mock SecurityContextHolder（认证用户）

mall-portal 很多 Service 方法调用 `memberService.getCurrentMember()`，它从 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 获取当前用户。

```java
@BeforeEach
void setUp() {
    // 构造已认证的安全上下文
    UmsMember member = new UmsMember();
    member.setId(1L);
    member.setUsername("testUser");
    MemberDetails memberDetails = new MemberDetails(member);

    Authentication auth = new UsernamePasswordAuthenticationToken(
        memberDetails, null, memberDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
}

@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();  // 清理，避免污染其他测试
}
```

### 7.2 如何测试 @Transactional 方法

Service 上的 `@Transactional` 在 Mockito 测试中**不会生效**（Spring 容器没启动）。只需验证：
- 方法的**业务逻辑**正确（条件分支、数据转换、返回值）
- Mapper 的**调用参数**正确（用 `verify` 或用 `ArgumentCaptor` 捕获参数）
- 不验证事务回滚行为（那是集成测试的事）

```java
@Test
void shouldCallInsertForEachOrderWhenDelivery() {
    // 验证 delivery 方法内部调用了 orderDao.delivery(...) 
    // 并且为每个订单插入了操作历史
    ArgumentCaptor<List<OmsOrderOperateHistory>> captor = ArgumentCaptor.forClass(List.class);
    when(orderDao.delivery(anyList())).thenReturn(2);

    service.delivery(deliveryParams);

    verify(orderOperateHistoryDao).insertList(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
}
```

### 7.3 PageHelper 分页方法怎么测

PageHelper 使用 `ThreadLocal`，必须在真实 MyBatis 上下文中才能工作。方案：
1. **Service 单元测试**：Mock PageHelper 行为（实际上 verify 调用就算覆盖了）
2. **集成测试**：用 `@SpringBootTest` + H2 真实走 MyBatis

```java
// 单元测试：只验证参数传递和返回值包装
@Test
void listShouldCallPageHelper() {
    // 无法在单元测试中真正启动 PageHelper，这不影响覆盖率
    // 在集成测试中补充这段即可
    List<Xxx> mockResult = Arrays.asList(new Xxx(), new Xxx());
    when(xxxMapper.selectByExample(any())).thenReturn(mockResult);

    List<Xxx> result = service.list("keyword", 1, 10);

    assertThat(result).hasSize(2);
    verify(xxxMapper).selectByExample(any());
}
```

### 7.4 JwtTokenUtil 测试技巧

```java
class JwtTokenUtilTest {
    private JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();

    @BeforeEach
    void setUp() {
        // 直接反射注入 @Value 属性，不启动 Spring
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "test-secret-key-128bits!!");
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 3600L);
        ReflectionTestUtils.setField(jwtTokenUtil, "tokenHead", "Bearer ");
    }

    @Test
    void tokenRoundTrip() {
        // 先造一个 UserDetails
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            "admin", "pwd", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // 生成 token
        String token = jwtTokenUtil.generateToken(userDetails);

        // 从 token 解析用户名
        String username = jwtTokenUtil.getUserNameFromToken(token);
        assertThat(username).isEqualTo("admin");

        // 校验 token
        assertThat(jwtTokenUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void expiredTokenShouldFailValidation() {
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", -1L);  // 立即过期
        // 验证过期 token 行为...
    }
}
```

---

## 8. 覆盖率配置

### 8.1 添加 JaCoCo 插件

在父 `pom.xml` 的 `<build><plugins>` 中添加（`<pluginManagement>` **外面**）：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 8.2 查看报告

```bash
mvn verify
# 打开各模块的 target/site/jacoco/index.html
```

---

## 9. 常见问题

### Q1：测试没执行，直接 SKIPPED？

父 POM 有 `<skipTests>true</skipTests>`，在命令行加 `-DskipTests=false`：

```bash
mvn test -DskipTests=false
```

或者在子模块 POM 中覆盖：

```xml
<properties>
    <skipTests>false</skipTests>
</properties>
```

### Q2：`@SpringBootTest` 报 "Unable to find a @SpringBootConfiguration"？

测试类包路径不在 `@SpringBootApplication` 所在包及其子包下。确保：
- `mall-admin` 的测试在 `com.macro.mall` 包下
- `mall-portal` 的测试在 `com.macro.mall.portal` 包下

### Q3：Mock 注入失败，`@InjectMocks` 的 Service 里有些字段是 null？

mall 项目所有 Service 使用**字段注入**（`@Autowired`），`@InjectMocks` 完全能处理。但要注意：
- 确保所有注入的字段都声明了对应的 `@Mock`
- 如果 Service 中有 `@Value` 注入的字段，用 `ReflectionTestUtils.setField(target, fieldName, value)` 手动设值

### Q4：H2 数据库 SQL 语法不兼容 MySQL？

H2 的 MySQL 兼容模式能处理大部分语法，但 `INSERT IGNORE`、`REPLACE INTO`、`ON DUPLICATE KEY UPDATE` 可能不兼容。遇到时：
- 用 `@AutoConfigureTestDatabase(replace = Replace.ANY)` 让 Spring Boot 自动适配
- 复杂 SQL 的测试可以用 Mock 代替真实 DB 访问

### Q5：Elasticsearch 测试怎么搞？

**不启动真实 ES**。mall-search 的单元测试全部 Mock：
- Mock `EsProductDao`（MyBatis 查询 MySQL → 不需要 DB）
- Mock `EsProductRepository`（Spring Data ES → 不需要 ES）
- Mock `ElasticsearchRestTemplate`（ES 客户端 → 不需要 ES）

---

## 附录：建议实施路线

```
第 1 步：先装 assertj 依赖（父 POM）
第 2 步：mall-common 全部 8 个测试类（打下基础）
第 3 步：mall-security 全部 10 个测试类（掌握 Mock/反射/安全测试）
第 4 步：mall-admin 30 个 Service 测试（大量重复模式，练手 Mockito）
第 5 步：mall-portal 16 个 Service 测试（掌握 SecurityContext Mock）
第 6 步：Web 层 MockMvc（admin 10 个 + portal 8 个 Controller）
第 7 步：mall-search 2 个测试类
第 8 步：配置 JaCoCo + 生成覆盖率报告
```

每个模块的测试写完一条命令就能跑：

```bash
mvn test -pl <模块名>
```

祝你刷出漂亮的覆盖率报告 🚀
