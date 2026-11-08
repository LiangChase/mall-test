package com.macro.mall.service.impl;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.dao.UmsAdminRoleRelationDao;
import com.macro.mall.dto.UmsAdminParam;
import com.macro.mall.dto.UpdateAdminPasswordParam;
import com.macro.mall.mapper.UmsAdminMapper;
import com.macro.mall.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.model.UmsAdminExample;
import com.macro.mall.security.util.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.macro.mall.service.UmsAdminCacheService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UmsAdminServiceImplTest {
    @Spy
    @InjectMocks
    UmsAdminServiceImpl umsAdminService;
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UmsAdminMapper adminMapper;
    @Mock
    private UmsAdminRoleRelationMapper adminRoleRelationMapper;
    @Mock
    private UmsAdminRoleRelationDao adminRoleRelationDao;
    @Mock
    private UmsAdminCacheService cacheService;
    @Mock
    private UserDetails userDetails;


//==================updatePasswordTest========================
    @Test
    @DisplayName("当修改信息输入不全为空时，修改失败，返回-1")
    void shouldReturnMinusOneWhenParamIsIncomplete(){
        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        {
            param.setNewPassword(null);
            param.setOldPassword(null);
            param.setUsername(null);
        }
        int result = umsAdminService.updatePassword(param);
        assertEquals(-1,result);
    }

    @Test
    @DisplayName("用户名不存在，修改失败，返回-2")
    void shouldReturnMinusTwoWhenUsernameNotFound(){
        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        {
            param.setNewPassword("123456");
            param.setOldPassword("654321");
            param.setUsername("ABC");
        }
        when(adminMapper.selectByExample(any(UmsAdminExample.class))).thenReturn(new ArrayList<>());

        int result = umsAdminService.updatePassword(param);

        assertEquals(-2,result);
    }

    @Test
    @DisplayName("旧密码填写错误，修改失败，返回-3")
    void shouldReturnMinusThreeWhenOldPasswordIncorrect(){
        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        {
            param.setNewPassword("123456");
            param.setOldPassword("123654");
            param.setUsername("ABC");
        }
        UmsAdmin admin = new UmsAdmin();
        admin.setPassword("123456");
        List<UmsAdmin> adminList = new ArrayList<>();
        {
            adminList.add(admin);
        };
        when(adminMapper.selectByExample(any(UmsAdminExample.class))).thenReturn(adminList);
        when(passwordEncoder.matches(any(),any())).thenReturn(false);
        int result = umsAdminService.updatePassword(param);
        assertEquals(-3,result);
    }

    @Test
    @DisplayName("密码修改成功")
    void shouldReturnOneWhenPasswordUpdatedSuccessfully(){
        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        {
            param.setNewPassword("123456");
            param.setOldPassword("123654");
            param.setUsername("ABC");
        }
        UmsAdmin admin = new UmsAdmin();
        admin.setPassword("123654");
        List<UmsAdmin> adminList = new ArrayList<>();
        {
            adminList.add(admin);
        };
        when(adminMapper.selectByExample(any(UmsAdminExample.class))).thenReturn(adminList);
        when(passwordEncoder.matches(any(),any())).thenReturn(true);
        when(passwordEncoder.encode(param.getNewPassword())).thenReturn(param.getNewPassword());
        when(adminMapper.updateByPrimaryKey(any())).thenReturn(1);
        doReturn(cacheService).when(umsAdminService).getCacheService();
        int result = umsAdminService.updatePassword(param);
        verify(adminMapper).updateByPrimaryKey(any());
        verify(passwordEncoder).encode(any());
        assertEquals(1,result);
    }


//=====================updateTest=============================
    @Test
    @DisplayName("新密码与原加密密码相同，密码不变")
    void shouldSetPasswordToNullWhenPasswordIsUnchanged(){
        UmsAdmin admin = new UmsAdmin();
        {
            admin.setId(1L);
            admin.setUsername("YongHu");
            admin.setPassword("123456");
        }
        UmsAdmin rawAdmin = new UmsAdmin();
        {
            rawAdmin.setId(1L);
            rawAdmin.setUsername("YongHu");
            rawAdmin.setPassword("123456");
        }
        when(adminMapper.selectByPrimaryKey(admin.getId())).thenReturn(rawAdmin);
        when(adminMapper.updateByPrimaryKeySelective(admin)).thenReturn(1);
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.update(1L,admin);

        assertEquals(1,count);
        assertNull(admin.getPassword());
        verify(adminMapper).selectByPrimaryKey(1L);
        verify(adminMapper).updateByPrimaryKeySelective(admin);
    }

    @Test
    @DisplayName("新密码与原密码不同且为空，密码不变")
    void shouldSetPasswordToNullWhenNewPasswordIsEmpty(){
        UmsAdmin admin = new UmsAdmin();
        {
            admin.setId(1L);
            admin.setUsername("YongHu");
            admin.setPassword(null);
        }
        UmsAdmin rawAdmin = new UmsAdmin();
        {
            rawAdmin.setId(1L);
            rawAdmin.setUsername("YongHu");
            rawAdmin.setPassword("123456");
        }
        when(adminMapper.selectByPrimaryKey(admin.getId())).thenReturn(rawAdmin);
        when(adminMapper.updateByPrimaryKeySelective(admin)).thenReturn(1);
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.update(1L,admin);

        assertEquals(1,count);
        assertNull(admin.getPassword());
        verify(adminMapper).selectByPrimaryKey(1L);
        verify(adminMapper).updateByPrimaryKeySelective(admin);
    }

    @Test
    @DisplayName("新密码与原密码不同且非空，修改原密码")
    void shouldEncodePasswordWhenNewPasswordIsNotEmpty(){
        UmsAdmin admin = new UmsAdmin();
        {
            admin.setId(1L);
            admin.setUsername("YongHu");
            admin.setPassword("654321");
        }
        UmsAdmin rawAdmin = new UmsAdmin();
        {
            rawAdmin.setId(1L);
            rawAdmin.setUsername("YongHu");
            rawAdmin.setPassword("123456");
        }
        when(adminMapper.selectByPrimaryKey(admin.getId())).thenReturn(rawAdmin);
        when(passwordEncoder.encode(admin.getPassword())).thenReturn("654321");
        when(adminMapper.updateByPrimaryKeySelective(admin)).thenReturn(1);
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.update(1L,admin);

        assertEquals(1,count);
        assertEquals("654321",admin.getPassword());
        verify(adminMapper).selectByPrimaryKey(1L);
        verify(adminMapper).updateByPrimaryKeySelective(admin);
        verify(passwordEncoder).encode("654321");
    }


//===========================registerTest===========================
    @Test
    @DisplayName("创建用户名已存在，创建失败")
    void shouldReturnNullWhenUsernameAlreadyExists(){
        UmsAdminParam umsAdminParam = new UmsAdminParam();
        {
            umsAdminParam.setUsername("Tom");
            umsAdminParam.setPassword("1234567890");
        }
        UmsAdmin admin = new UmsAdmin();
        {
            admin.setId(1L);
            admin.setUsername("Tom");
            admin.setPassword("654321");
        }
        List<UmsAdmin> umsAdminList = new ArrayList<>();
        umsAdminList.add(admin);
        when(adminMapper.selectByExample(any())).thenReturn(umsAdminList);

        UmsAdmin result = umsAdminService.register(umsAdminParam);

        verify(adminMapper).selectByExample(any());
        assertNull(result);

    }

    @Test
    @DisplayName("创建用户名不存在，创建成功")
    void shouldRegisterSuccessfullyWhenUsernameDoesNotExist(){
        UmsAdminParam umsAdminParam = new UmsAdminParam();
        {
            umsAdminParam.setUsername("Tom");
            umsAdminParam.setPassword("1234567890");
        }
        List<UmsAdmin> umsAdminList = new ArrayList<>();
        when(adminMapper.selectByExample(any())).thenReturn(umsAdminList);
        when(passwordEncoder.encode("1234567890")).thenReturn("5656566361156456");

        UmsAdmin result = umsAdminService.register(umsAdminParam);

        verify(passwordEncoder).encode("1234567890");
        verify(adminMapper).selectByExample(any());
        assertEquals("Tom",result.getUsername());
        assertEquals("5656566361156456",result.getPassword());
    }


//=====================updateRoleTest=========================
    @Test
    @DisplayName("角色列表为null时，只删除旧关系、不插入新关系，返回 0")
    void shouldOnlyDeleteWhenRoleIdsIsNull(){
        Long adminId = 8L;
        List<Long> roleIds = null;
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.updateRole(adminId,roleIds);

        assertEquals(0,count);
        verify(adminRoleRelationMapper).deleteByExample(any());
        verify(adminRoleRelationDao,never()).insertList(any());
        verify(cacheService).delResourceList(adminId);
    }

    @Test
    @DisplayName("角色列表为空时，只删除旧关系、不插入新关系，返回 0")
    void shouldOnlyDeleteWhenRoleIdsIsEmpty(){
        Long adminId = 8L;
        List<Long> roleIds = new ArrayList<>();
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.updateRole(adminId,roleIds);

        assertEquals(0,count);
        verify(adminRoleRelationMapper).deleteByExample(any());
        verify(adminRoleRelationDao,never()).insertList(any());
        verify(cacheService).delResourceList(adminId);
    }

    @Test
    @DisplayName("角色列表非空时，先删除旧关系再插入新关系，返回角色数量")
    void shouldDeleteOldAndInsertNewWhenRoleIdsIsNotEmpty(){
        Long adminId = 8L;
        List<Long> roleIds = new ArrayList<>();
        {
            roleIds.add(1L);
            roleIds.add(2L);
            roleIds.add(3L);
        }
        doReturn(cacheService).when(umsAdminService).getCacheService();

        int count = umsAdminService.updateRole(adminId,roleIds);

        assertEquals(3,count);
        InOrder inOrder = inOrder(adminRoleRelationMapper,adminRoleRelationDao);
        inOrder.verify(adminRoleRelationMapper).deleteByExample(any());
        inOrder.verify(adminRoleRelationDao).insertList(any());
        verify(cacheService).delResourceList(adminId);
    }


//=============================loginTest==================================
    @Test
    @DisplayName("密码不正确，登录返回异常")
    void shouldThrowApiExceptionWhenPasswordIsWrong(){
        String username = "User";
        String password = "123456";
        doReturn(userDetails).when(umsAdminService).loadUserByUsername(username);
        when(passwordEncoder.matches(any(),any())).thenReturn(false);

        ApiException e = assertThrows(ApiException.class,() -> umsAdminService.login(username,password));
        verify(passwordEncoder).matches(any(),any());
        assertEquals("密码不正确",e.getMessage());
    }

    @Test
    @DisplayName("账号已被禁用，登录返回异常")
    void shouldThrowApiExceptionWhenAccountIsDisabled(){
        String username = "User";
        String password = "123456";
        doReturn(userDetails).when(umsAdminService).loadUserByUsername(username);
        when(passwordEncoder.matches(any(),any())).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(false);

        ApiException e = assertThrows(ApiException.class,() -> umsAdminService.login(username,password));
        verify(passwordEncoder).matches(any(),any());
        assertEquals("帐号已被禁用",e.getMessage());
    }

    @Test
    @DisplayName("密码正确且账号未被禁用时，登录成功，应返回 token")
    void shouldReturnTokenWhenLoginSuccessfully(){
        String username = "User";
        String password = "123456";
        doReturn(userDetails).when(umsAdminService).loadUserByUsername(username);
        doReturn(cacheService).when(umsAdminService).getCacheService();
        when(passwordEncoder.matches(any(),any())).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn("TokenTest");

        String token = umsAdminService.login(username,password);
        verify(passwordEncoder).matches(any(),any());
        assertEquals("TokenTest",token);
    }

    @Test
    @DisplayName("用户名不存在时，UsernameNotFoundException 被捕获，登录失败，应返回 null")
    void shouldReturnNullWhenThrowUsernameNotFoundException(){
        String username = "User";
        String password = "123456";
        doThrow(new UsernameNotFoundException("用户名错误")).when(umsAdminService).loadUserByUsername(username);

        String token = umsAdminService.login(username,password);

        assertNull(token);
        verify(passwordEncoder,never()).matches(any(),any());
        verify(jwtTokenUtil,never()).generateToken(any());

    }


//======================loadUserByUsernameTest===========================
    @Test
    @DisplayName("用户名不存在时，应抛出 UsernameNotFoundException")
    void loadUserByUsernameTest(){
        String username = "User";
        doReturn(null).when(umsAdminService).getAdminByUsername(username);

        assertThatThrownBy(() -> umsAdminService.loadUserByUsername(username)).isInstanceOf(UsernameNotFoundException.class).hasMessage("用户名或密码错误");
    }
}
