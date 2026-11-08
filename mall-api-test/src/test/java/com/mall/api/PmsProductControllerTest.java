package com.mall.api;

import com.mall.tool.DbUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class PmsProductControllerTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TEST_PRODUCT_SN = "TEST-001";
    private static final String TEST_PRODUCT_NAME = "测试商品";

    private static String token = "";

    //登录获取token
    @BeforeAll
    static void getLoginToken() {
        Response loginResponse =
                given()
                        .contentType("application/json")
                        .body("""
                                {"username":"test","password":"123456"}
                                """)
                .when()
                        .post(BASE_URL + "/admin/login");

        token = loginResponse.jsonPath().getString("data.token");
        assertNotNull(token, "登录失败，未获取到 token");
    }

    @AfterEach
    void clean() {
        DbUtil.execute("DELETE FROM pms_product WHERE product_sn = ?", TEST_PRODUCT_SN);
    }

    //===================== 私有工具 =====================

    private Response createTestProduct() {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "name": "测试商品",
                          "productSn": "TEST-001",
                          "price": 99.00,
                          "stock": 100,
                          "deleteStatus": 0,
                          "publishStatus": 1
                        }
                        """)
                .when()
                        .post(BASE_URL + "/product/create")
                .then()
                        .extract().response();
    }

    private long queryProductId() {
        return given()
                .header("Authorization", "Bearer " + token)
                .queryParam("productSn", TEST_PRODUCT_SN)
                .when()
                        .get(BASE_URL + "/product/list")
                .then()
                        .extract().jsonPath().getLong("data.list[0].id");
    }

    //===================== create =====================

    //createTest(创建商品测试)
    @Test
    @DisplayName("当商品创建成功时，返回成功信号与添加的商品数")
    void shouldReturnSuccessWhenCreateSuccess() {
        Response response = createTestProduct();

        int http_code = response.getStatusCode();
        int bus_code = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");
        int count = response.jsonPath().getInt("data");

        assertEquals(200, http_code);
        assertEquals(200, bus_code);
        assertEquals("操作成功", message);
        assertEquals(1, count);
    }

    @Test
    @DisplayName("参数全为 null 时，服务器返回 500，暴露未做参数校验的缺陷")
    void shouldReturn500WhenAllParamsAreNull() {
        Response response =
                given().
                        header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        body("{" +
                                "\"name\": null," +
                                "\"productSn\": null," +
                                "\"price\": null," +
                                "\"stock\": null" +
                                "}").
                        when().
                        post(BASE_URL + "/product/create").
                        then().
                        extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(500, response.jsonPath().getInt("code"));
        assertTrue(response.jsonPath().getString("message").contains("SQL syntax"));
        assertNull(response.jsonPath().get("data"));
    }

    //===================== list =====================

    @Test
    @DisplayName("创建商品后，按货号查询能查到该商品")
    void shouldReturnCreatedProductWhenQueryByProductSn() {
        createTestProduct();

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .queryParam("productSn", TEST_PRODUCT_SN)
                .when()
                        .get(BASE_URL + "/product/list")
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data.total"));
        assertEquals(TEST_PRODUCT_SN, response.jsonPath().getString("data.list[0].productSn"));
        assertEquals(TEST_PRODUCT_NAME, response.jsonPath().getString("data.list[0].name"));
    }

    //===================== updateInfo =====================

    @Test
    @DisplayName("根据商品 id 获取编辑信息，返回商品详情")
    void shouldReturnProductDetailWhenIdExists() {
        createTestProduct();
        long id = queryProductId();

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                .when()
                        .get(BASE_URL + "/product/updateInfo/" + id)
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(TEST_PRODUCT_NAME, response.jsonPath().getString("data.name"));
        assertEquals(TEST_PRODUCT_SN, response.jsonPath().getString("data.productSn"));
        assertEquals(99.00, response.jsonPath().getDouble("data.price"));
    }
}
