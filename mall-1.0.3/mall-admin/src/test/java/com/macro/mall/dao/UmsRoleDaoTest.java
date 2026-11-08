package com.macro.mall.dao;

import com.macro.mall.model.UmsAdminRoleRelation;
import com.macro.mall.model.UmsMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.macro.mall.dao.UmsAdminRoleRelationDaoTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class UmsRoleDaoTest {
    @Autowired
    UmsRoleDao umsRoleDao;

    @Autowired
    UmsAdminRoleRelationDao umsAdminRoleRelationDao;

    @Test
    @DisplayName("测试菜单获取能否返回正确的用户信息")
    void getMenuListTest(){
        Long adminId_1L = 1L;
        List<UmsMenu> list_1L = umsRoleDao.getMenuList(adminId_1L);
        assertThat(list_1L).extracting(UmsMenu::getId).containsExactlyInAnyOrder(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,11L,12L,13L,14L,16L,17L,18L,19L,20L,21L,22L,23L,24L,25L);
        Long adminId_6L = 6L;
        List<UmsMenu> list_6L = umsRoleDao.getMenuList(adminId_6L);
        assertThat(list_6L).extracting(UmsMenu::getId).containsExactlyInAnyOrder(1L,2L,3L,4L,5L,6L);
        Long adminId_7L = 7L;
        List<UmsMenu> list_7L = umsRoleDao.getMenuList(adminId_7L);
        assertThat(list_7L).extracting(UmsMenu::getId).containsExactlyInAnyOrder(7L,8L,9L,10L,11L);
        Long adminId_2L = 2L;
        List<UmsMenu> list_2L = umsRoleDao.getMenuList(adminId_2L);
        assertThat(list_2L).extracting(UmsMenu::getId).isEmpty();

        UmsAdminRoleRelation r1 = UmsAdminRoleRelationDaoTest.createUmsAdminRoleRelation(100L, 9L, 5L);
        UmsAdminRoleRelation r2 = UmsAdminRoleRelationDaoTest.createUmsAdminRoleRelation(101L, 9L, 1L);
        List<UmsAdminRoleRelation> relations = new ArrayList<>();
        relations.add(r1);
        relations.add(r2);
        umsAdminRoleRelationDao.insertList(relations);

        List<UmsMenu> list_9L = umsRoleDao.getMenuList(9L);
        assertThat(list_9L).hasSize(24);
        assertThat(list_9L).extracting(UmsMenu::getId).doesNotHaveDuplicates();
    }
}
