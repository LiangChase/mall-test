package com.mall.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UmsAdminLoginApiTest {
    @Test
    void loginWithWrongAccountTest(){
        Response response =
                given().
                when().
                get("http://localhost:8080").
                then().extract().response();
        int busCode = response.jsonPath().getInt("code");
        int httpCode = response.getStatusCode();

        assertEquals(200,httpCode);
        assertEquals(401,busCode);
    }
}
