package com.macro.mall.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


import javax.servlet.http.HttpServletRequest;

import java.net.InetAddress;

@ExtendWith(MockitoExtension.class)
public class RequestUtilTest {

    @Mock
    private HttpServletRequest request;


    @Test
    @DisplayName("所有if分支都不符合时")
    void shouldReturnOriginalIpAddress(){
        when(request.getHeader("x-forwarded-for")).thenReturn("127.0.0.1");

        String ipAddress = RequestUtil.getRequestIp(request);

        verify(request).getHeader("x-forwarded-for");

        assertEquals("127.0.0.1",ipAddress);
    }

    @Test
    @DisplayName("进入第一个if分支后返回ipAddress")
    void shouldEntryFirstIf(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("127.0.0.1");

        String ipAddress = RequestUtil.getRequestIp(request);

        verify(request).getHeader("x-forwarded-for");
        verify(request).getHeader("Proxy-Client-IP");

        assertEquals("127.0.0.1",ipAddress);
    }

    @Test
    @DisplayName("进入第二个if分支后返回ipAddress")
    void shouldEntrySecondIf(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("127.0.0.1");

        String ipAddress =  RequestUtil.getRequestIp(request);

        verify(request).getHeader("x-forwarded-for");
        verify(request).getHeader("Proxy-Client-IP");
        verify(request).getHeader("WL-Proxy-Client-IP");

        assertEquals("127.0.0.1",ipAddress);
    }

    @Test
    @DisplayName("进入第三个if后立刻返回ipAddress")
    void shouldEntryThirdIf(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("198.168.0.1");

        String ipAddress = RequestUtil.getRequestIp(request);

        verify(request).getHeader("x-forwarded-for");
        verify(request).getHeader("Proxy-Client-IP");
        verify(request).getHeader("WL-Proxy-Client-IP");
        verify(request).getRemoteAddr();

        assertEquals("198.168.0.1",ipAddress);
    }

    @Test
    @DisplayName("测试unknown能否被正确触发")
    void testUnknown(){
        when(request.getHeader("x-forwarded-for")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("198.168.0.1");

        String ipAddress = RequestUtil.getRequestIp(request);

        verify(request).getHeader("x-forwarded-for");
        verify(request).getHeader("Proxy-Client-IP");
        verify(request).getHeader("WL-Proxy-Client-IP");
        verify(request).getRemoteAddr();

        assertEquals("198.168.0.1",ipAddress);
    }

    @Test
    @DisplayName("进入第三个if且ipAddress是127.0.0.1")
    void shouldEntryThirdIfII(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        InetAddress inetAddress = mock(InetAddress.class);

        try(MockedStatic<InetAddress> mockedStatic = mockStatic(InetAddress.class)){
            mockedStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);

            when(inetAddress.getHostAddress()).thenReturn("192.168.0.1");

            String ipAddress = RequestUtil.getRequestIp(request);

            verify(request).getHeader("x-forwarded-for");
            verify(request).getHeader("Proxy-Client-IP");
            verify(request).getHeader("WL-Proxy-Client-IP");
            verify(request).getRemoteAddr();
            verify(inetAddress).getHostAddress();

            assertEquals("192.168.0.1",ipAddress);
        }
    }

    @Test
    @DisplayName("进入第三个if且ipAddress是'0:0:0:0:0:0:0:1'")
    void shouldEntryThirdIfIII(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        InetAddress inetAddress = mock(InetAddress.class);

        try(MockedStatic<InetAddress> mockedStatic = mockStatic(InetAddress.class)){
            mockedStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);

            when(inetAddress.getHostAddress()).thenReturn("192.168.0.1");

            String ipAddress = RequestUtil.getRequestIp(request);

            verify(request).getHeader("x-forwarded-for");
            verify(request).getHeader("Proxy-Client-IP");
            verify(request).getHeader("WL-Proxy-Client-IP");
            verify(request).getRemoteAddr();
            verify(inetAddress).getHostAddress();

            assertEquals("192.168.0.1",ipAddress);
        }
    }

    @Test
    @DisplayName("IP分隔测试")
    void testIpSegmentation(){
        when(request.getHeader("x-forwarded-for")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("198.168.0.1,127.5.2.1,156.7.5.1");

        String ipAddress = RequestUtil.getRequestIp(request);

        assertEquals("198.168.0.1",ipAddress);

    }
}
