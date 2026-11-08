package com.macro.mall.controller;

import com.macro.mall.common.pojo.Book;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    @GetMapping("/te/st")
    public String testAOP(){
        String str = "TestAOP";
        return str;
    }

    @GetMapping("/test/requestparam")
    @ApiOperation("获取用户信息")
    public String testParam(@RequestParam("name") String name){
        return "RequestParam-ok";
    }

    @PostMapping("/test/requestbody")
    @ApiOperation("发送图书信息")
    public String testParam(@RequestBody Book book){
        return "RequestBody-ok";
    }

}
