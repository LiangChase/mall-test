package com.macro.mall.service.impl;

import com.macro.mall.dto.UmsMenuNode;
import com.macro.mall.mapper.UmsMenuMapper;
import com.macro.mall.model.UmsMenu;
import com.macro.mall.model.UmsMenuExample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UmsMenuServiceImplTest {
    @InjectMocks
    private UmsMenuServiceImpl umsMenuService;

    @Mock
    private UmsMenuMapper menuMapper;


    private UmsMenu createUmsMenu(Long id,Long parentId){
        UmsMenu umsMenu = new UmsMenu();
        umsMenu.setId(id);
        umsMenu.setParentId(parentId);
        return umsMenu;
    }
    UmsMenu umsMenu1 = createUmsMenu(1L,0L);
    UmsMenu umsMenu2 = createUmsMenu(2L,1L);
    UmsMenu umsMenu3 = createUmsMenu(3L,1L);
    UmsMenu umsMenu4 = createUmsMenu(4L,2L);

    private List<UmsMenu> menuList = new ArrayList<>();
    {
        menuList.add(umsMenu1);
        menuList.add(umsMenu2);
        menuList.add(umsMenu3);
        menuList.add(umsMenu4);
    };

    @Test
    @DisplayName("将扁平菜单列表按 parentId 正确构建为树形结构，父子节点正确挂载")
    void treeListTest(){
        when(menuMapper.selectByExample(any(UmsMenuExample.class))).thenReturn(menuList);
        List<UmsMenuNode> result = umsMenuService.treeList();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getChildren().size()).isEqualTo(2);
        assertThat(result.get(0).getChildren().get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("创建一级菜单（parentId=0）时，level 应为 0")
    void shouldSetLevelToZeroWhenParentIdIsZero(){
        UmsMenu umsMenu = createUmsMenu(1L,0L);

        umsMenuService.create(umsMenu);

        assertThat(umsMenu.getLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("创建有父菜单的子菜单时，level 应为父菜单 level + 1")
    void shouldSetLevelToParentLevelPlusOneWhenParentMenuExists(){
        UmsMenu umsMenu = createUmsMenu(1L,0L);
        UmsMenu umsMenu1 = createUmsMenu(2L,1L);

        umsMenu.setLevel(0);

        when(menuMapper.selectByPrimaryKey(1L)).thenReturn(umsMenu);

        umsMenuService.create(umsMenu1);
        assertThat(umsMenu1.getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("父菜单不存在时，level 应回退为 0 ")
    void shouldSetLevelToZeroWhenParentMenuNotFound(){
        UmsMenu umsMenu1 = createUmsMenu(2L,1L);

        when(menuMapper.selectByPrimaryKey(1L)).thenReturn(null);

        umsMenuService.create(umsMenu1);
        assertThat(umsMenu1.getLevel()).isEqualTo(0);
    }
}
