package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallUserVO;
import ltd.newbee.mall.dao.MallUserMapper;
import ltd.newbee.mall.entity.MallUser;
import ltd.newbee.mall.util.MD5Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallUserServiceImplTest {

    @Mock
    private MallUserMapper mallUserMapper;

    @InjectMocks
    private NewBeeMallUserServiceImpl userService;

    @Test
    void register_withAvailableLoginName_returnsSuccessAndStoresMd5Password() {
        when(mallUserMapper.selectByLoginName("newbee")).thenReturn(null);
        when(mallUserMapper.insertSelective(any(MallUser.class))).thenReturn(1);

        String result = userService.register("newbee", "password");

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        ArgumentCaptor<MallUser> captor = ArgumentCaptor.forClass(MallUser.class);
        verify(mallUserMapper).insertSelective(captor.capture());
        MallUser savedUser = captor.getValue();
        assertEquals("newbee", savedUser.getLoginName());
        assertEquals("newbee", savedUser.getNickName());
        assertEquals(MD5Util.MD5Encode("password", "UTF-8"), savedUser.getPasswordMd5());
    }

    @Test
    void register_withExistingLoginName_returnsSameLoginNameMessage() {
        when(mallUserMapper.selectByLoginName("newbee")).thenReturn(ServiceTestFixtures.mallUser(1L, "newbee", "newbee", 0));

        String result = userService.register("newbee", "password");

        assertEquals(ServiceResultEnum.SAME_LOGIN_NAME_EXIST.getResult(), result);
    }

    @Test
    void login_withValidCredentials_setsUserSessionAndReturnsSuccess() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = ServiceTestFixtures.mallUser(1L, "newbee", "nick", 0);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        NewBeeMallUserVO sessionUser = (NewBeeMallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        assertNotNull(sessionUser);
        assertEquals(1L, sessionUser.getUserId());
        assertEquals("newbee", sessionUser.getLoginName());
        assertEquals("nick", sessionUser.getNickName());
    }

    @Test
    void login_withNicknameLongerThanSeven_truncatesNicknameInSession() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = ServiceTestFixtures.mallUser(1L, "newbee", "12345678", 0);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        NewBeeMallUserVO sessionUser = (NewBeeMallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        assertEquals("1234567..", sessionUser.getNickName());
    }

    @Test
    void login_withNicknameLengthSeven_doesNotTruncateNickname() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = ServiceTestFixtures.mallUser(1L, "newbee", "1234567", 0);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        NewBeeMallUserVO sessionUser = (NewBeeMallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        assertEquals("1234567", sessionUser.getNickName());
    }

    @Test
    void login_withWrongCredentials_returnsLoginError() {
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", "bad-md5")).thenReturn(null);

        String result = userService.login("newbee", "bad-md5", new MockHttpSession());

        assertEquals(ServiceResultEnum.LOGIN_ERROR.getResult(), result);
    }

    @Test
    void login_withLockedUser_returnsLockedMessageAndDoesNotSetSession() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = ServiceTestFixtures.mallUser(1L, "newbee", "nick", 1);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        assertEquals(ServiceResultEnum.LOGIN_USER_LOCKED.getResult(), result);
        assertNull(session.getAttribute(Constants.MALL_USER_SESSION_KEY));
    }

    @Test
    void updateUserInfo_whenDatabaseUpdateSucceeds_returnsUpdatedUserAndRefreshesSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.MALL_USER_SESSION_KEY, ServiceTestFixtures.userVO(1L));
        MallUser userFromDb = ServiceTestFixtures.mallUser(1L, "newbee", "old", 0);
        MallUser updateRequest = new MallUser();
        updateRequest.setNickName("newnick");
        updateRequest.setAddress("newaddress");
        updateRequest.setIntroduceSign("newsign");
        when(mallUserMapper.selectByPrimaryKey(1L)).thenReturn(userFromDb);
        when(mallUserMapper.updateByPrimaryKeySelective(userFromDb)).thenReturn(1);

        NewBeeMallUserVO result = userService.updateUserInfo(updateRequest, session);

        assertNotNull(result);
        assertEquals("newnick", result.getNickName());
        assertEquals("newaddress", result.getAddress());
        assertEquals("newsign", result.getIntroduceSign());
        assertSame(result, session.getAttribute(Constants.MALL_USER_SESSION_KEY));
    }

    @Test
    void updateUserInfo_whenSessionUserMissing_returnsNull() {
        MockHttpSession session = new MockHttpSession();
        MallUser updateRequest = new MallUser();
        updateRequest.setNickName("newNick");

        NewBeeMallUserVO result = userService.updateUserInfo(updateRequest, session);

        assertNull(result);
        verifyNoInteractions(mallUserMapper);
    }

    @Test
    void updateUserInfo_whenDatabaseUserMissing_returnsNull() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.MALL_USER_SESSION_KEY, ServiceTestFixtures.userVO(1L));
        MallUser updateRequest = new MallUser();
        updateRequest.setNickName("newNick");
        when(mallUserMapper.selectByPrimaryKey(1L)).thenReturn(null);

        NewBeeMallUserVO result = userService.updateUserInfo(updateRequest, session);

        assertNull(result);
    }

    @Test
    void lockUsers_withIdsAndSuccessfulMapperUpdate_returnsTrue() {
        Integer[] ids = new Integer[]{1, 2};
        when(mallUserMapper.lockUserBatch(eq(ids), eq(1))).thenReturn(2);

        Boolean result = userService.lockUsers(ids, 1);

        assertTrue(result);
    }

    @Test
    void lockUsers_withEmptyIds_returnsFalse() {
        Boolean result = userService.lockUsers(new Integer[]{}, 1);

        assertEquals(Boolean.FALSE, result);
    }
}
