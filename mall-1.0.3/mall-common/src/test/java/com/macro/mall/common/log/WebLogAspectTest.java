package com.macro.mall.common.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.json.JSONUtil;
import com.macro.mall.common.pojo.Book;
import com.macro.mall.controller.TestController;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@WebMvcTest
@Import({WebLogAspect.class, TestController.class})
@EnableAspectJAutoProxy
public class WebLogAspectTest{
    @Autowired
    private MockMvc mockMvc;

    private WebLogAspect webLogAspect = new WebLogAspect();

    // 声明一个内存中的日志附加器，用于捕获切面打印的日志
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup(){
        Logger logger = (Logger)LoggerFactory.getLogger(WebLogAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("请求命中切点时，切面的 @Before、@Around、@AfterReturning 三个通知均被触发")
    void shouldTriggerAllAdvicesWhenControllerInvoked() throws Exception{
        //执行一次真实的HTTP GET请求(spring模拟环境)
        mockMvc.perform(MockMvcRequestBuilders.get("/te/st"));

        List<ILoggingEvent> logsList = listAppender.list;

        assertThat(logsList).isNotEmpty();

        String logMessage = logsList.get(0).getFormattedMessage();

        assertThat(logMessage).contains("te/st");
        assertThat(logMessage).contains("spendTime");
        assertThat(logMessage).contains("TestAOP");

        JSONObject json = JSONUtil.parseObj(logMessage);
        assertThat(json.getStr("parameter")).isNull();
        assertThat(json.getStr("description")).isNull();
    }

    @Test
    @DisplayName("请求命中切点时，doAround正确记录uri、method、result、参数(@RequestParam)、描述")
    void shouldRecordRequestDetailsWithRequestParam() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/test/requestparam").param("name","test"));

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).isNotEmpty();

        String logMessage = logsList.get(0).getFormattedMessage();
        JSONObject json = JSONUtil.parseObj(logMessage);

        assertThat(json.getStr("uri")).isEqualTo("/test/requestparam");
        assertThat(json.getStr("method")).isEqualTo("GET");
        assertThat(json.getStr("result")).isEqualTo("RequestParam-ok");

        assertThat(json.getStr("description")).isEqualTo("获取用户信息");

        assertThat(json.getStr("parameter")).contains("\"name\":\"test\"");
    }

    private Book book = new Book(1,"计算机","如何成为一名合格测试开发工程师","测开方向就业指南");

    @Test
    @DisplayName("请求命中切点时，doAround正确记录uri、method、result、参数(@RequestBody)、描述")
    void shouldRecordRequestDetailsWithRequestBody() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.post("/test/requestbody").contentType(MediaType.APPLICATION_JSON).content(JSONUtil.toJsonStr(book)));

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).isNotEmpty();

        String logMessage = logsList.get(0).getFormattedMessage();
        JSONObject json = JSONUtil.parseObj(logMessage);

        assertThat(json.getStr("uri")).isEqualTo("/test/requestbody");
        assertThat(json.getStr("method")).isEqualTo("POST");
        assertThat(json.getStr("result")).isEqualTo("RequestBody-ok");

        assertThat(json.getStr("description")).isEqualTo("发送图书信息");

        JSONObject paramJson = json.getJSONObject("parameter");
        assertThat(paramJson.getInt("id")).isEqualTo(1);
        assertThat(paramJson.getStr("type")).isEqualTo("计算机");
        assertThat(paramJson.getStr("name")).isEqualTo("如何成为一名合格测试开发工程师");
        assertThat(paramJson.getStr("description")).isEqualTo("测开方向就业指南");
    }

}
