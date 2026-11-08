package com.macro.mall.common.api;

import org.springframework.data.domain.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommonPageTest {
    List<Integer> list = new ArrayList<>();
    {
      list.add(1);
      list.add(2);
      list.add(3);
      list.add(4);
      list.add(5);
    }

    @Mock
    Page<Integer> pageInfo;

    @Test
    @DisplayName("将 PageHelper 的 List 转换为分页信息")
    void testListConvertPagingInformation(){
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3));
        CommonPage<Integer> result = CommonPage.restPage(list);

        assertEquals(3,result.getTotal());
        assertEquals(3,result.getPageSize());
        assertEquals(1,result.getTotalPage());
        assertEquals(1,result.getPageNum());
        assertEquals(list,result.getList());

    }

    @Test
    @DisplayName("将 Spring Data 的 Page 转换为分页信息")
    void testPageConvertPagingInformation(){
        when(pageInfo.getTotalPages()).thenReturn(5);
        when(pageInfo.getNumber()).thenReturn(4);
        when(pageInfo.getSize()).thenReturn(3);
        when(pageInfo.getTotalElements()).thenReturn((long)6);
        when(pageInfo.getContent()).thenReturn(list);

        CommonPage<Integer> result = CommonPage.restPage(pageInfo);

        assertEquals(5,result.getTotalPage());
        assertEquals(4,result.getPageNum());
        assertEquals(3,result.getPageSize());
        assertEquals(6,result.getTotal());
        assertEquals(list,result.getList());
    }
}
