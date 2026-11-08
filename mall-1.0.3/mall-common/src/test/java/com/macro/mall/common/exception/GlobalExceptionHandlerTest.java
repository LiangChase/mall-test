package com.macro.mall.common.exception;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.common.api.IErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.sql.SQLSyntaxErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {
    @Mock
    SQLSyntaxErrorException e;

    @Mock
    ApiException a;

    @Mock
    IErrorCode b;

    @Mock
    MethodArgumentNotValidException mE;

    @Mock
    BindingResult bindingResult;

    @Mock
    BindException bE;

    FieldError fieldError = new FieldError("objectName","field","defaultMessage");

    @InjectMocks
    GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("SQL 异常 message 含 denied 时返回'演示环境暂无修改权限，如需修改数据可本地搭建后台服务！'")
    void handleSqlSyntaxErrorExceptionWithDenied(){
        when(e.getMessage()).thenReturn("denied");

        CommonResult commonResult = globalExceptionHandler.handleSQLSyntaxErrorException(e);

        assertEquals("演示环境暂无修改权限，如需修改数据可本地搭建后台服务！",commonResult.getMessage());
        assertEquals(500,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("SQL 异常 message 不含 denied 时返回原始信息")
    void handleSqlSyntaxErrorExceptionWithoutDenied(){
        when(e.getMessage()).thenReturn("hello");

        CommonResult commonResult = globalExceptionHandler.handleSQLSyntaxErrorException(e);

        assertEquals("hello",commonResult.getMessage());
        assertEquals(500,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("BindException 有字段错误时返回字段名+错误信息")
    void handleBindExceptionWithFieldError(){
        when(bE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        CommonResult commonResult = globalExceptionHandler.handleValidException(bE);

        assertEquals("fielddefaultMessage",commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("BindException 无字段错误时返回 null message")
    void handleBindExceptionWithoutFieldError(){
        when(bE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(true);

        when(bindingResult.getFieldError()).thenReturn(null);

        CommonResult commonResult = globalExceptionHandler.handleValidException(bE);

        verify(bindingResult).getFieldError();
        assertEquals(null,commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("BindException 无校验错误时返回 null message")
    void handleBindExceptionNoError(){
        when(bE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(false);

        CommonResult commonResult = globalExceptionHandler.handleValidException(bE);

        verify(bE).getBindingResult();

        assertEquals(null,commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test //主分支测试
    @DisplayName("@Valid 参数校验失败时返回字段名+错误信息，测试handleValidException(MethodArgumentNotValidException e)")
    void handleMethodArgumentNotValidWithFieldError(){
        when(mE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(true);

        when(bindingResult.getFieldError()).thenReturn(fieldError);

        CommonResult commonResult = globalExceptionHandler.handleValidException(mE);

        assertEquals("fielddefaultMessage",commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("@Valid 校验异常但无字段错误时返回 null message")
    void handleMethodArgumentNotValidWithoutFieldError(){
        when(mE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(true);

        when(bindingResult.getFieldError()).thenReturn(null);

        CommonResult commonResult = globalExceptionHandler.handleValidException(mE);

        verify(bindingResult).getFieldError();
        assertEquals(null,commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("@Valid 校验无错误时返回 null message")
    void handleMethodArgumentNotValidNoError(){
        when(mE.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(false);

        CommonResult commonResult = globalExceptionHandler.handleValidException(mE);

        verify(mE).getBindingResult();

        assertEquals(null,commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("ApiException 无 errorCode 时返回自定义 message")
    void handleApiExceptionWithoutErrorCode(){
        when(a.getErrorCode()).thenReturn(null);
        when(a.getMessage()).thenReturn("IS_NULL_TEST");

        CommonResult commonResult = globalExceptionHandler.handle(a);

        assertEquals("IS_NULL_TEST",commonResult.getMessage());
        assertEquals(500,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

    @Test
    @DisplayName("ApiException 携带 errorCode 时返回对应错误码")
    void handleApiExceptionWithErrorCode(){
        when(a.getErrorCode()).thenReturn(b);
        when(b.getCode()).thenReturn((long)404);
        when(b.getMessage()).thenReturn("NOT_NULL_TEST");

        CommonResult commonResult = globalExceptionHandler.handle(a);

        assertEquals("NOT_NULL_TEST",commonResult.getMessage());
        assertEquals(404,commonResult.getCode());
        assertEquals(null,commonResult.getData());
    }

}
