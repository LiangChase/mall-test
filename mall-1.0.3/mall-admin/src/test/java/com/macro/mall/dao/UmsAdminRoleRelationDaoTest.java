package com.macro.mall.dao;

import com.macro.mall.model.UmsAdminRoleRelation;
import com.macro.mall.model.UmsResource;
import com.macro.mall.model.UmsRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class UmsAdminRoleRelationDaoTest {
    @Autowired
    UmsAdminRoleRelationDao umsAdminRoleRelationDao;

    public static UmsAdminRoleRelation createUmsAdminRoleRelation(Long id,Long adminId,Long roleId){
        UmsAdminRoleRelation umsAdminRoleRelation = new UmsAdminRoleRelation();
        umsAdminRoleRelation.setId(id);
        umsAdminRoleRelation.setAdminId(adminId);
        umsAdminRoleRelation.setRoleId(roleId);
        return umsAdminRoleRelation;
    }

    @Test
    @DisplayName("测试是否能够拿到库的数据")
    void getAdminIdListTest(){
        List<Long> list = umsAdminRoleRelationDao.getAdminIdList(1L);
        assertThat(list.size()).isEqualTo(5);
        assertThat(list).containsExactlyInAnyOrder(4L, 1L, 3L, 8L, 6L);
    }

    @Test
    @DisplayName("测试是否能够正确获取用户所有角色")
    void getRoleListTest(){
        List<UmsRole> list_1L = umsAdminRoleRelationDao.getRoleList(1L);
        List<UmsRole> list_3L = umsAdminRoleRelationDao.getRoleList(3L);
        List<UmsRole> list_4L = umsAdminRoleRelationDao.getRoleList(4L);
        List<UmsRole> list_6L = umsAdminRoleRelationDao.getRoleList(6L);
        List<UmsRole> list_7L = umsAdminRoleRelationDao.getRoleList(7L);
        List<UmsRole> list_8L = umsAdminRoleRelationDao.getRoleList(8L);

        assertThat(list_1L).extracting(UmsRole::getId).containsExactly(5L);
        assertThat(list_3L).extracting(UmsRole::getId).containsExactly(5L);
        assertThat(list_4L).extracting(UmsRole::getId).containsExactly(5L);
        assertThat(list_6L).extracting(UmsRole::getId).containsExactly(1L);
        assertThat(list_7L).extracting(UmsRole::getId).containsExactly(2L);
        assertThat(list_8L).extracting(UmsRole::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("测试是否能够插入用户角色关系")
    void insertListTest(){
        UmsAdminRoleRelation u1 = createUmsAdminRoleRelation(32L,9L,5L);
        UmsAdminRoleRelation u2 = createUmsAdminRoleRelation(33L,10L,1L);
        UmsAdminRoleRelation u3 = createUmsAdminRoleRelation(34L,11L,2L);

        List<UmsAdminRoleRelation> list = new ArrayList<>();
        {
            list.add(u1);
            list.add(u2);
            list.add(u3);
        }

        int count = umsAdminRoleRelationDao.insertList(list);

        assertThat(count).isEqualTo(3);
        assertThat(umsAdminRoleRelationDao.getRoleList(9L)).extracting(UmsRole::getId).contains(5L);
        assertThat(umsAdminRoleRelationDao.getRoleList(10L)).extracting(UmsRole::getId).contains(1L);
        assertThat(umsAdminRoleRelationDao.getRoleList(11L)).extracting(UmsRole::getId).contains(2L);
    }

    @Test
    @DisplayName("测试是否能够获取用户所有可访问资源")
    void getResourceListTest(){
        List<UmsResource> list_7L = umsAdminRoleRelationDao.getResourceList(7L);
        List<UmsResource> list_6L = umsAdminRoleRelationDao.getResourceList(6L);
        List<UmsResource> list_1L = umsAdminRoleRelationDao.getResourceList(1L);

        assertThat(list_7L).extracting(UmsResource::getId).containsExactlyInAnyOrder(8L,9L,10L,11L,12L,31L,32L);
        assertThat(list_6L).extracting(UmsResource::getId).containsExactlyInAnyOrder(1L,2L,3L,4L,5L,6L,23L,24L,31L,32L);
        assertThat(list_1L).extracting(UmsResource::getId).hasSize(29).doesNotHaveDuplicates();

        UmsAdminRoleRelation u1 = createUmsAdminRoleRelation(32L,999L,1L);
        UmsAdminRoleRelation u2 = createUmsAdminRoleRelation(33L,999L,2L);
        List<UmsAdminRoleRelation> list = new ArrayList<>();
        list.add(u1);
        list.add(u2);

        umsAdminRoleRelationDao.insertList(list);
        List<UmsResource> list_999L = umsAdminRoleRelationDao.getResourceList(999L);

        assertThat(list_999L).extracting(UmsResource::getId).hasSize(15).doesNotHaveDuplicates();
    }
}

