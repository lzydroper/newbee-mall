package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallUserVO;
import ltd.newbee.mall.dao.MallUserMapper;
import ltd.newbee.mall.entity.MallUser;
import ltd.newbee.mall.service.impl.NewBeeMallUserServiceImpl;
import ltd.newbee.mall.util.MD5Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoundaryNewBeeMallUserTest {

    @Mock
    private MallUserMapper mallUserMapper;

    @InjectMocks
    private NewBeeMallUserServiceImpl userService;

    @Test
    void login_withNicknameLengthSeven_doesNotTruncateNickname() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = BoundaryTestFixtures.mallUser(1L, "newbee", "1234567", 0);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        NewBeeMallUserVO sessionUser = (NewBeeMallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals("1234567", sessionUser.getNickName());
    }

    @Test
    void login_withNicknameLengthEight_truncatesNickname() {
        String passwordMd5 = MD5Util.MD5Encode("password", "UTF-8");
        MallUser user = BoundaryTestFixtures.mallUser(1L, "newbee", "12345678", 0);
        MockHttpSession session = new MockHttpSession();
        when(mallUserMapper.selectByLoginNameAndPasswd("newbee", passwordMd5)).thenReturn(user);

        String result = userService.login("newbee", passwordMd5, session);

        NewBeeMallUserVO sessionUser = (NewBeeMallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals("1234567..", sessionUser.getNickName());
    }
}
