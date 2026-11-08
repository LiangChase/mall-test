package com.macro.mall.common.service.impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisServiceImplTest {
    //==============准备测试数据==============
    String key = "user:1001";
    String hashKey = "name";
    String value = "张三";
    long time = 60L;
    Map<String,Object> map = new HashMap<>();
    {
        map.put("电视", 3000);
        map.put("手机", 3800);
        map.put("电脑", 13800);
    }
    Object[] values = {"张三","李四","王五"};

    @Mock
    private RedisTemplate<String,Object> redisTemplate;

    @InjectMocks
    private RedisServiceImpl redisService;

    @Mock
    private HashOperations<String,Object,Object> hashOperations;

    @Mock
    private SetOperations setOps;

    @Mock
    private ListOperations listOps;

    @BeforeEach
    void setUp(){
        //共同所需的mock
        when(redisTemplate.expire(key,time,TimeUnit.SECONDS)).thenReturn(true);

    }

    @Test
    @DisplayName("hSet ———— 存入 Hash 并设置过期时间")
    void testHSetWithTime(){
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        Boolean result = redisService.hSet(key,hashKey,value,time);

        assertTrue(result);

        verify(hashOperations).put(key,hashKey,value);

        verify(redisTemplate).expire(key,time,TimeUnit.SECONDS);

    }

    @Test
    @DisplayName("hSetAll ———— 批量存入整个 Map 并设置过期时间批量存入整个 Map 并设置过期时间")
    void testHSetAll(){
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Boolean result = redisService.hSetAll(key,map,time);
        assertTrue(result);

        verify(hashOperations).putAll(key,map);

        verify(redisTemplate).expire(key,time,TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("sAdd ———— 向 Set 中添加元素并设置过期时间")
    void testSAdd(){
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(key,values)).thenReturn(time);
        Long count = redisService.sAdd(key,time,values);
        assertEquals(count,time);

        verify(setOps).add(key,values);
        verify(redisTemplate).expire(key,time,TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("lPush ———— 向 List 尾部添加元素并设置过期时间")
    void testlpush(){
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.rightPush(key,value)).thenReturn((long)1);
        Long index = redisService.lPush(key,value,time);
        verify(listOps).rightPush(key,value);
        verify(redisTemplate).expire(key,time,TimeUnit.SECONDS);

        assertEquals((long)1,index);
    }

    @Test
    @DisplayName("lPushAll ———— 批量向 List 尾部添加多个元素并设置过期时间")
    void testlPushAll(){
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.rightPushAll(key,values)).thenReturn((long)1);

        Long count = redisService.lPushAll(key,time,values);

        verify(listOps).rightPushAll(key,values);
        verify(redisTemplate).expire(key,time,TimeUnit.SECONDS);


        assertEquals((long)1,count);
    }
}
