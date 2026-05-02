package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.dao.AdminUserMapper;
import ltd.newbee.mall.entity.AdminUser;
import ltd.newbee.mall.util.MD5Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void login_withCorrectCredentials_queriesByMd5PasswordAndReturnsAdmin() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "password");
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        when(adminUserMapper.login("admin", passwordMd5)).thenReturn(adminUser);

        AdminUser result = adminUserService.login("admin", "password");

        assertSame(adminUser, result);
        verify(adminUserMapper).login("admin", passwordMd5);
    }

    @Test
    void login_withWrongCredentials_returnsNull() {
        String passwordMd5 = MD5Util.MD5Encode("wrong", "UTF-8");
        when(adminUserMapper.login("admin", passwordMd5)).thenReturn(null);

        AdminUser result = adminUserService.login("admin", "wrong");

        assertNull(result);
    }

    @Test
    void updatePassword_withCorrectOriginalPassword_updatesPasswordAndReturnsTrue() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "oldPassword");
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(adminUser);
        when(adminUserMapper.updateByPrimaryKeySelective(adminUser)).thenReturn(1);

        Boolean result = adminUserService.updatePassword(1, "oldPassword", "newPassword");

        assertTrue(result);
        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateByPrimaryKeySelective(captor.capture());
        assertEquals(MD5Util.MD5Encode("newPassword", "UTF-8"), captor.getValue().getLoginPassword());
    }

    @Test
    void updatePassword_withWrongOriginalPassword_returnsFalse() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "oldPassword");
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(adminUser);

        Boolean result = adminUserService.updatePassword(1, "wrong", "newPassword");

        assertFalse(result);
    }

    @Test
    void updatePassword_whenAdminMissing_returnsFalse() {
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(null);

        Boolean result = adminUserService.updatePassword(1, "oldPassword", "newPassword");

        assertFalse(result);
    }

    @Test
    void updatePassword_whenDatabaseUpdateFails_returnsFalse() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "oldPassword");
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(adminUser);
        when(adminUserMapper.updateByPrimaryKeySelective(adminUser)).thenReturn(0);

        Boolean result = adminUserService.updatePassword(1, "oldPassword", "newPassword");

        assertFalse(result);
    }

    @Test
    void updateName_withExistingAdmin_updatesNameAndReturnsTrue() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "password");
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(adminUser);
        when(adminUserMapper.updateByPrimaryKeySelective(adminUser)).thenReturn(1);

        Boolean result = adminUserService.updateName(1, "newLoginName", "newNick");

        assertTrue(result);
        verify(adminUserMapper).updateByPrimaryKeySelective(eq(adminUser));
        assertEquals("newLoginName", adminUser.getLoginUserName());
        assertEquals("newNick", adminUser.getNickName());
    }

    @Test
    void updateName_whenAdminMissing_returnsFalse() {
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(null);

        Boolean result = adminUserService.updateName(1, "newLoginName", "newNick");

        assertFalse(result);
    }

    @Test
    void updateName_whenDatabaseUpdateFails_returnsFalse() {
        AdminUser adminUser = ServiceTestFixtures.adminUser(1, "password");
        when(adminUserMapper.selectByPrimaryKey(1)).thenReturn(adminUser);
        when(adminUserMapper.updateByPrimaryKeySelective(adminUser)).thenReturn(0);

        Boolean result = adminUserService.updateName(1, "newLoginName", "newNick");

        assertFalse(result);
    }
}
