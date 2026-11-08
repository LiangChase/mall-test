package com.mall.api;

import com.mall.tool.DbUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class UmsMenuControllerTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TEST_MENU_TITLE = "api_test_menu";
    private static final String TEST_SUB_MENU_TITLE = "api_test_menu_child";

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
        DbUtil.execute("DELETE FROM ums_menu WHERE title LIKE ?", "api_test_menu%");
    }

    //===================== 私有工具 =====================

    private Response createMenu(long parentId, String title, int sort) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "parentId": %d,
                          "title": "%s",
                          "name": "apiTestMenu",
                          "sort": %d,
                          "hidden": 0
                        }
                        """.formatted(parentId, title, sort))
                .when()
                        .post(BASE_URL + "/menu/create")
                .then()
                        .extract().response();
    }

    /**
     * 按 parentId 分页查询菜单，从返回列表中找到指定 title 的菜单 id
     */
    private long queryMenuId(long parentId, String title) {
        List<Map<String, Object>> menuList =
                given()
                        .header("Authorization", "Bearer " + token)
                        .queryParam("pageSize", 100)
                        .queryParam("pageNum", 1)
                .when()
                        .get(BASE_URL + "/menu/list/" + parentId)
                .then()
                        .extract().jsonPath().getList("data.list");

        return menuList.stream()
                .filter(menu -> title.equals(menu.get("title")))
                .map(menu -> ((Number) menu.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到测试菜单：" + title));
    }

    //===================== create =====================

    @Test
    @DisplayName("添加一级菜单成功，返回影响行数 1")
    void shouldReturnSuccessWhenCreateTopLevelMenu() {
        Response response = createMenu(0L, TEST_MENU_TITLE, 0);

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));
    }

    @Test
    @DisplayName("创建子菜单时，level 为父菜单 level + 1")
    void shouldSetLevelToParentLevelPlusOneWhenParentMenuExists() {
        //先创建一级菜单(level = 0)
        createMenu(0L, TEST_MENU_TITLE, 0);
        long parentId = queryMenuId(0L, TEST_MENU_TITLE);

        //再创建子菜单
        Response response = createMenu(parentId, TEST_SUB_MENU_TITLE, 0);
        assertEquals(1, response.jsonPath().getInt("data"));

        //查询子菜单，验证 level = 1
        List<Map<String, Object>> childList =
                given()
                        .header("Authorization", "Bearer " + token)
                        .queryParam("pageSize", 100)
                        .queryParam("pageNum", 1)
                .when()
                        .get(BASE_URL + "/menu/list/" + parentId)
                .then()
                        .extract().jsonPath().getList("data.list");

        Map<String, Object> childMenu = childList.stream()
                .filter(menu -> TEST_SUB_MENU_TITLE.equals(menu.get("title")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到子菜单"));

        assertEquals(1, ((Number) childMenu.get("level")).intValue());
        assertEquals(parentId, ((Number) childMenu.get("parentId")).longValue());
    }

    //===================== getItem =====================

    @Test
    @DisplayName("根据 id 获取菜单详情成功")
    void shouldReturnMenuWhenIdExists() {
        createMenu(0L, TEST_MENU_TITLE, 0);
        long id = queryMenuId(0L, TEST_MENU_TITLE);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                .when()
                        .get(BASE_URL + "/menu/" + id)
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(TEST_MENU_TITLE, response.jsonPath().getString("data.title"));
        assertEquals(0, response.jsonPath().getInt("data.level"));
    }

    //===================== treeList =====================

    @Test
    @DisplayName("树形结构返回菜单列表，包含新创建的一级菜单")
    void shouldContainCreatedMenuInTreeList() {
        createMenu(0L, TEST_MENU_TITLE, 0);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                .when()
                        .get(BASE_URL + "/menu/treeList")
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));

        List<Map<String, Object>> treeList = response.jsonPath().getList("data");
        assertTrue(treeList.stream().anyMatch(node -> TEST_MENU_TITLE.equals(node.get("title"))),
                "树形菜单中未找到新创建的菜单");
    }

    //===================== delete =====================

    @Test
    @DisplayName("删除菜单成功，删除后查询不到")
    void shouldReturnSuccessWhenDeleteMenu() {
        createMenu(0L, TEST_MENU_TITLE, 0);
        long id = queryMenuId(0L, TEST_MENU_TITLE);

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                .when()
                        .post(BASE_URL + "/menu/delete/" + id)
                .then()
                        .extract().response();

        assertEquals(200, response.getStatusCode());
        assertEquals(200, response.jsonPath().getInt("code"));
        assertEquals(1, response.jsonPath().getInt("data"));

        //验证已删除
        Response detailResponse =
                given()
                        .header("Authorization", "Bearer " + token)
                .when()
                        .get(BASE_URL + "/menu/" + id)
                .then()
                        .extract().response();

        assertNull(detailResponse.jsonPath().get("data"));
    }
}
