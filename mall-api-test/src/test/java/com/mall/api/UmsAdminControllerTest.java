package com.mall.api;

import com.mall.tool.DbUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class UmsAdminControllerTest {

    @AfterEach
    void cleanUpData(){
        DbUtil.execute("Delete From ums_admin Where username like ?","api_test_%");
        DbUtil.execute("DELETE FROM ums_admin WHERE TRIM(username) = ?","");
    }

//=====================loginTest======================
    //登录失败，无参的情况
    @Test
    @DisplayName("应该返回400，当无参时")
    void shouldFailWhenBodyIsMiss(){
        Response response =
            given().
                contentType("application/json").
            when().
                post("http://localhost:8080/admin/login").
            then().
                extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("status");

        assertEquals(400,httpCode);
        assertEquals(400,busCode);

    }

    //当用户不存在时，登录理应失败
    @Test
    @DisplayName("应该返回404，当用户不存在时")
    void shouldFailWhenUserDoesNotExist(){
        Response response =
                given().
                    contentType("application/json").
                    body("{\"username\":\"user\",\"password\":\"123456\"}").
                when().
                    post("http://localhost:8080/admin/login").
                then().
                    extract().response();
        int httpCode = response.statusCode();
        int busCode = response.jsonPath().getInt("code");
        //参数完整，理应成功运行，HttpCode == 200
        assertEquals(200,httpCode);
        //用户信息不在数据库当中(验证失败)，登录不会成功
        assertEquals(404,busCode);
        assertEquals("用户名或密码错误",response.jsonPath().getString("message"));
    }

    //当用户存在且密码错误时，抛ApiException，被全局异常处理器捕获处理
    @Test
    @DisplayName("应该返回500，当密码错误时")
    void shouldFailWhenPasswordIsWrong(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"test\",\"password\": \"654321\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(500,busCode);
        assertEquals("密码不正确",message);
    }

    @Test
    @DisplayName("应该返回500，当账号被禁用时")
    void shouldFailWhenAccountHasBeenDisabled(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_DisAccount\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"被禁用的用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        int register_http_code = response.getStatusCode();
        int register_bus_code = response.jsonPath().getInt("code");
        Integer id = response.jsonPath().getInt("data.id");
        String register_message = response.jsonPath().getString("message");

        assertEquals(200,register_http_code);
        assertEquals(200,register_bus_code);
        assertEquals("操作成功",register_message);

        Response loginResponse =
                given().
                        contentType("application/json").
                        body("{\"username\":\"test\",\"password\":\"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        String token = loginResponse.jsonPath().getString("data.token");


        Response response_updateStatus =
                given().
                        header("Authorization", "Bearer " + token).
                when().
                        post("http://localhost:8080/admin/updateStatus/" + id + "?status=0").
                then().
                        extract().response();


        int upSta_http_code = response_updateStatus.getStatusCode();
        int upSta_bus_code = response_updateStatus.jsonPath().getInt("code");
        String upSta_message = response_updateStatus.jsonPath().getString("message");

        assertEquals(200,upSta_http_code);
        assertEquals(200,upSta_bus_code);
        assertEquals("操作成功",upSta_message);

        Response response_login =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_DisAccount\",\"password\": \"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        int login_http_code = response_login.getStatusCode();
        int login_bus_code = response_login.jsonPath().getInt("code");
        String login_message = response_login.jsonPath().getString("message");

        assertEquals(200,login_http_code);
        assertEquals(500,login_bus_code);
        assertEquals("帐号已被禁用",login_message);
    }

    @Test
    @DisplayName("当用户存在且密码正确时，应该登录成功")
    void shouldSuccessWhenUserInformationIsCorrect(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"test\",\"password\": \"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");
        String token = response.jsonPath().getString("data.token");

        assertEquals(200,httpCode);
        assertEquals(200,busCode);
        assertEquals("操作成功",message);
        assertNotNull(token);
    }


//=================================registerTest=================================
    @Test
    @DisplayName("用户名已存在时，注册应该失败")
    void shouldFailWhenUsernameAlreadyExists(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"test\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        //“test”用户已经存在，所以adminService.register会返回null,导致注册失败
        //返回FAILED的code：500，message：操作失败
        assertEquals(500,busCode);
        assertEquals("操作失败",message);
    }

    //=================对于用户名过滤机制的测试==========================
    @Test
    @DisplayName("测试是否能够识别参数异常(用户名为空)")
    void shouldFailRegisterWhenUserNameIsEmpty(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        //当应户名为空时，校验后应抛出MethodArgumentNotValidException e，返回CommonResult.validateFailed(message);
        assertEquals(200,httpCode);
        assertEquals(404,busCode);//参数校验失败
        assertEquals("username不能为空",message);
    }

    //检测出一处异常，对于username限制的纰漏，可将@NotEmpty改成@NotBlank，过滤多个空格的结果
    @Test
    @DisplayName("证明项目在username注册填写时的纰漏了多个空格的情况")
    void shouldFailRegisterWhenTheUserNameIsSpace() {
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"  \"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                        when().
                        post("http://localhost:8080/admin/register").
                        then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

    /*UserAdminParam给username的限制仅仅是@NotEmpty，拦不住多个空格，所以在应该断言为404的测试中，
    会报错，因为他拦截不到，让多个空格的不合规用户名过去了*/
        assertEquals(200,httpCode);
        assertEquals(404,busCode);
        assertEquals("username不能为空",message);
    }

    //==================对于password过滤机制的测试========================
    @Test
    @DisplayName("测试password是否能过滤纯空的字符")
    void shouldFailRegisterWhenPasswordIsEmpty(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(404,busCode);//参数校验失败
        assertEquals("password不能为空",message);
    }

    //测试失败，发现password过滤机制有漏洞，可以改用@NotBlank
    @Test
    @DisplayName("测试是否能过滤内容为多个空格的password")
    void shouldFailRegisterWhenPasswordIsSpaces(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"    \"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                        when().
                        post("http://localhost:8080/admin/register").
                        then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(404,busCode);
        assertEquals("password不能为空",message);//应该被拦截返回404，却返回200，拦截机制有缺失
    }

    //======================测试邮箱格式过滤机制===========================
    @Test
    @DisplayName("测试当email项填入的格式不符合邮箱格式时，是否会被拦截注册")
    void shouldFailRegisterWhenEmailFormalError(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        /*busCode应该返回404，因为UmsAdminParma在eamil上有@Email注释，规定了要遵循邮箱格式
        当前用例email显然不满足此条件，被异常处理器拦截*/
        assertEquals(200,httpCode);
        assertEquals(404,busCode);//参数解析失败
        assertEquals("email不是一个合法的电子邮件地址",message);
    }

    @Test
    @DisplayName("注册成功")
    void registerSuccessfully(){
        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        //用户名没有重复，能够成功注册
        assertEquals(200,httpCode);
        assertEquals(200,busCode);
        assertEquals("操作成功",message);
    }


//===============================getAdminInfoTest===================================
    @Test
    @DisplayName("参数(principal)为空时，是否能够return出来")
    void shouldReturnUnauthorizedWhenPrincipalIsNull(){
        Principal principal;
        Response response =
                given().
                when().
                        get("http://localhost:8080/admin/info").
                then().
                        extract().response();
        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(401,busCode);
        assertEquals("暂未登录或token已经过期",message);
    }

    @Test
    @DisplayName("查询成功且有数据时")
    void shouldSuccessfulAndRoleListNotEmpty(){
        //getAdminInfo不在白名单内，需要先登录拿到token才能获取用户的信息
        Response login_Response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"test\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        String token = login_Response.jsonPath().getString("data.token");

        Response info_Response =
                given().
                        header("Authorization", "Bearer " + token).
                when().
                        get("http://localhost:8080/admin/info").
                then().
                        log().all().extract().response();

        int http_code = info_Response.getStatusCode();
        int bus_code = info_Response.jsonPath().getInt("code");
        String message = info_Response.jsonPath().getString("message");
        //获取得到信息的数量
        int menus_size = info_Response.jsonPath().getList("data.menus").size();
        String roles = info_Response.jsonPath().getString("data.roles");
        String icon = info_Response.jsonPath().getString("data.icon");

        assertEquals(200,http_code);
        assertEquals(200,bus_code);
        assertEquals("操作成功",message);
        //在数据库中使用对应查询语句得到结果为24
        assertEquals(24,menus_size);
        assertEquals("[超级管理员]",roles);
        assertEquals("https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/icon/github_icon_02.png",icon);
    }

    @Test
    @DisplayName("查询成功且无数据，登录用户id=2时，符合情况")
    void shouldSuccessfulAndRoleListIsEmpty(){
        Response login_Response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"test256\",\"password\": \"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        String token = login_Response.jsonPath().getString("data.token");

        Response info_Response =
                given().
                        header("Authorization", "Bearer " + token).
                when().
                        get("http://localhost:8080/admin/info").
                then().
                        log().all().extract().response();

        int http_code = info_Response.getStatusCode();
        int bus_code = info_Response.jsonPath().getInt("code");
        String message = info_Response.jsonPath().getString("message");
        //获得到的信息的数量
        int menus_size = info_Response.jsonPath().getList("data.menus").size();
        String roles = info_Response.jsonPath().getString("data.roles");
        String icon = info_Response.jsonPath().getString("data.icon");


        assertEquals(200,http_code);
        assertEquals(200,bus_code);
        assertEquals("操作成功",message);
        //在数据库查询中使用对应sql语句，得到结果0
        assertEquals(0,menus_size);
        assertEquals("string",icon);
    }


//===========================updatePasswordTest=================================
    @Test
    @DisplayName("没有登入时，无法进行更改密码的操作")
    void shouldFailWhenThereIsNoLogin(){
        //创建一个新用户测试updatepassword以免影响数据库
        Response register_response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        Response response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\",\"oldPassword\": \"123456\",\"newPassword\": \"654321\"}").
                when().
                        post("http://localhost:8080/admin/updatePassword").
                then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(401,busCode);
        assertEquals("暂未登录或token已经过期",message);
    }

    @Test
    @DisplayName("提交参数不合法时，不能成功更改密码")
    /*源代码中预设的是某参数为空status返回-1，抛fail，但是由于在@Valid校验参数不为空的情况下,
    status等于-1的情况不可能被触发，会优先被全局异常处理器拦截抛404：参数校验失败*/
    void shouldFailWhenSubmittingParametersIsIllegal(){
        //创建一个新用户测试updatepassword以免影响数据库
        Response register_response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        //updatePassword不在白名单上，需要具备登录状态
        Response login_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"test\",\"password\":\"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        //获取登录状态
        String token = login_response.jsonPath().getString("data.token");

        Response response =
                given().
                        //传入登录状态
                                header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        //newPassword为空的情况
                        body("{\"username\": \"api_test_user\",\"oldPassword\": \"123456\",\"newPassword\": \"\"}").
               when().
                        post("http://localhost:8080/admin/updatePassword").
               then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(404,busCode);
        assertEquals("newPassword不能为空",message);
    }

    @Test
    @DisplayName("修改用户不存在时，无法操作成功")
    void shouldFailWhenTheModifiedUserDoesNotExist(){
        //updatePassword不在白名单上，需要具备登录状态
        Response login_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"test\",\"password\":\"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        //获取登录状态
        String token = login_response.jsonPath().getString("data.token");

        Response response =
                given().
                        //传入登录状态
                        header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        //newPassword为空的情况
                        body("{\"username\": \"api_test_abc\",\"oldPassword\": \"123456\",\"newPassword\": \"654321\"}").
                when().
                        post("http://localhost:8080/admin/updatePassword").
                then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(500,busCode);
        assertEquals("找不到该用户",message);
    }

    @Test
    @DisplayName("当旧密码输入错误时，不允许修改密码")
    void shouldFailWhenOldPasswordError(){
        //创建一个新用户测试updatepassword以免影响数据库
        Response register_response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                        when().
                        post("http://localhost:8080/admin/register").
                        then().
                        extract().response();

        //updatePassword不在白名单上，需要具备登录状态
        Response login_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"test\",\"password\":\"123456\"}").
                        when().
                        post("http://localhost:8080/admin/login").
                        then().
                        extract().response();

        //获取登录状态
        String token = login_response.jsonPath().getString("data.token");

        Response response =
                given().
                        //传入登录状态
                                header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\",\"oldPassword\": \"231321654\",\"newPassword\": \"654321\"}").
                        when().
                        post("http://localhost:8080/admin/updatePassword").
                        then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(500,busCode);
        assertEquals("旧密码错误",message);
    }

    @Test
    @DisplayName("提交参数合法且登录时，成功更改密码")
    void shouldFailWhenModifyPasswordWithTheNewlyCreatedUser(){
        //创建一个新用户测试updatepassword以免影响数据库
        Response register_response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                        when().
                        post("http://localhost:8080/admin/register").
                        then().
                        extract().response();

        //updatePassword不在白名单上，需要具备登录状态
        Response login_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"api_test_user\",\"password\":\"123456\"}").
                        when().
                        post("http://localhost:8080/admin/login").
                        then().
                        extract().response();

        //获取登录状态
        String token = login_response.jsonPath().getString("data.token");

        Response response =
                given().
                        //传入登录状态
                                header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\",\"oldPassword\": \"123456\",\"newPassword\": \"654321\"}").
                        when().
                        post("http://localhost:8080/admin/updatePassword").
                        then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");

        assertEquals(200,httpCode);
        assertEquals(403,busCode);
        assertEquals("没有相关权限",message);
    }

    @Test
    @DisplayName("提交参数合法且登录时，成功更改密码")
    void shouldSuccessWhenSubmittingParametersIsLegalAndBeLoggedIn(){
        //创建一个新用户测试updatepassword以免影响数据库
        Response register_response =
                given().
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\"," +
                                "\"password\": \"123456\"," +
                                "\"icon\": \"string\"," +
                                "\"email\": \"123456@qq.com\"," +
                                "\"nickName\": \"testAccount\"," +
                                "\"note\":\"用户\"}").
                when().
                        post("http://localhost:8080/admin/register").
                then().
                        extract().response();

        //updatePassword不在白名单上，需要具备登录状态
        Response login_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"test\",\"password\":\"123456\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        //获取登录状态
        String token = login_response.jsonPath().getString("data.token");

        Response response =
                given().
                        //传入登录状态
                        header("Authorization", "Bearer " + token).
                        contentType("application/json").
                        body("{\"username\": \"api_test_user\",\"oldPassword\": \"123456\",\"newPassword\": \"654321\"}").
                when().
                        post("http://localhost:8080/admin/updatePassword").
                then().
                        log().all().extract().response();

        int httpCode = response.getStatusCode();
        int busCode = response.jsonPath().getInt("code");
        String message = response.jsonPath().getString("message");
        int data = response.jsonPath().getInt("data");

        assertEquals(200,httpCode);
        assertEquals(200,busCode);
        assertEquals("操作成功",message);
        assertEquals(1,data);

        //用更改后的密码进行登入，验证密码更改成功
        Response verification_response =
                given().
                        contentType("application/json").
                        body("{\"username\":\"api_test_user\",\"password\":\"654321\"}").
                when().
                        post("http://localhost:8080/admin/login").
                then().
                        extract().response();

        int verification_code = verification_response.jsonPath().getInt("code");
        assertEquals(200,verification_code);
    }
}
