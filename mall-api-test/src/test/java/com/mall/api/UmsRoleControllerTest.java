package com.mall.api;

import com.mall.tool.DbUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class UmsRoleControllerTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TEST_ROLE_NAME = "api_test_role";
    private static final String TEST_ROLE_NAME_UPDATED = "api_test_role_updated";

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
    void cleanUpData() {
        DbUtil.execute("DELETE FROM ums_role WHERE name LIKE ?", "api_test_role%");
    }

    //===================== 私有工具 =====================

    private Response createTestRole() {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "name": "api_test_role",
                          "description": "接口测试创建的角色",
                          "status": 1,
                          "sort": 0
                        }
                        """)
                .when()
                        .post(BASE_URL + "/role/create")
                .then()
                        .extract().response();
    }

    private long queryRoleId(String keyword) {
        return given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", keyword)
                .when()
                        .get(BASE_URL + "/role/list")
                .then()
                        .extract().jsonPath().getLong("data.list[0].id");
    }

    private Response queryRoleList(String keyword) {
        return given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", keyword)
                .when()
                        .get(BASE_URL + "/role/list")
                .then()
                        .extract().response();
    }

    //===================== create =====================

    @Test
    @DisplayName("添加角色成功，返回影响行数 1")
    void shouldReturnSuccessWhenCreateRole() {
        Response response = createTestRole();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));
    }

    //===================== list =====================

    @Test
    @DisplayName("按角色名称模糊查询，返回对应角色")
    void shouldReturnRoleWhenQueryByKeyword() {
        createTestRole();

        Response response = queryRoleList(TEST_ROLE_NAME);

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data.total"));
        assertEquals(TEST_ROLE_NAME, response.jsonPath().getString("data.list[0].name"));
    }

    //===================== update =====================

    @Test
    @DisplayName("修改角色成功，返回影响行数 1，修改后查询到新名称")
    void shouldReturnSuccessWhenUpdateRole() {
        createTestRole();
        long id = queryRoleId(TEST_ROLE_NAME);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body("""
                                {
                                  "name": "api_test_role_updated",
                                  "description": "更新后的描述"
                                }
                                """)
                .when()
                        .post(BASE_URL + "/role/update/" + id)
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));

        //验证修改结果
        Response listResponse = queryRoleList(TEST_ROLE_NAME_UPDATED);
        assertEquals(TEST_ROLE_NAME_UPDATED, listResponse.jsonPath().getString("data.list[0].name"));
        assertEquals("更新后的描述", listResponse.jsonPath().getString("data.list[0].description"));
    }

    //===================== updateStatus =====================

    @Test
    @DisplayName("修改角色状态为禁用，状态生效")
    void shouldUpdateRoleStatusToDisabled() {
        createTestRole();
        long id = queryRoleId(TEST_ROLE_NAME);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .queryParam("status", 0)
                .when()
                        .post(BASE_URL + "/role/updateStatus/" + id)
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));

        //验证状态已改为禁用(0)
        Response listResponse = queryRoleList(TEST_ROLE_NAME);
        assertEquals(0, listResponse.jsonPath().getInt("data.list[0].status"));
    }

    //===================== delete =====================

    @Test
    @DisplayName("删除角色成功，删除后查询不到")
    void shouldReturnSuccessWhenDeleteRole() {
        createTestRole();
        long id = queryRoleId(TEST_ROLE_NAME);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .queryParam("ids", id)
                .when()
                        .post(BASE_URL + "/role/delete")
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));

        //验证已删除
        Response listResponse = queryRoleList(TEST_ROLE_NAME);
        assertEquals(0, listResponse.jsonPath().getInt("data.total"));
    }
}
