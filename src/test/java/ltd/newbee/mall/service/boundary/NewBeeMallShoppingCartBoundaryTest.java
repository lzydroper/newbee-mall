package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallShoppingCartItem;
import ltd.newbee.mall.service.impl.NewBeeMallShoppingCartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallShoppingCartBoundaryTest {

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallShoppingCartServiceImpl shoppingCartService;

    @Test
    void saveNewBeeMallCartItem_withGoodsCountEqualLimit_returnsSuccess() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(1L, 1L, 10L, Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(BoundaryTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(0);
        when(shoppingCartItemMapper.insertSelective(item)).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_withGoodsCountOneAboveLimit_returnsLimitMessage() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(1L, 1L, 10L, Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER + 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(BoundaryTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(0);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_whenUserHasTwelveItems_allowsOneMoreItem() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(1L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(BoundaryTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER - 1);
        when(shoppingCartItemMapper.insertSelective(item)).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_whenUserHasThirteenItems_rejectsOneMoreItem() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(1L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(BoundaryTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_TOTAL_NUMBER_ERROR.getResult(), result);
    }

    @Test
    void getMyShoppingCartItems_withGoodsNameLengthTwentyEight_doesNotTruncateName() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(100L, 1L, 10L, 2);
        String boundaryName = BoundaryTestFixtures.repeat('g', 28);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, boundaryName);
        when(shoppingCartItemMapper.selectByUserId(1L, Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER)).thenReturn(Collections.singletonList(item));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertEquals(1, result.size());
        assertEquals(boundaryName, result.get(0).getGoodsName());
    }

    @Test
    void getMyShoppingCartItems_withGoodsNameLengthTwentyNine_truncatesName() {
        NewBeeMallShoppingCartItem item = BoundaryTestFixtures.cartItem(100L, 1L, 10L, 2);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, BoundaryTestFixtures.repeat('g', 29));
        when(shoppingCartItemMapper.selectByUserId(1L, Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER)).thenReturn(Collections.singletonList(item));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertEquals(1, result.size());
        assertEquals(BoundaryTestFixtures.repeat('g', 28) + "...", result.get(0).getGoodsName());
    }
}
