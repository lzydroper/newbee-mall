package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallShoppingCartItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallShoppingCartServiceImplTest {

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallShoppingCartServiceImpl shoppingCartService;

    @Test
    void saveNewBeeMallCartItem_withExistingGoodsAndCartBelowLimit_returnsSuccess() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(1L, 1L, 10L, Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(12);
        when(shoppingCartItemMapper.insertSelective(item)).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_whenSameGoodsAlreadyInCart_updatesOriginalItemQuantity() {
        NewBeeMallShoppingCartItem request = ServiceTestFixtures.cartItem(null, 1L, 10L, 3);
        NewBeeMallShoppingCartItem existing = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        NewBeeMallShoppingCartItem existingFromDb = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(existing);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(existingFromDb);
        when(shoppingCartItemMapper.updateByPrimaryKeySelective(existingFromDb)).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(request);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        ArgumentCaptor<NewBeeMallShoppingCartItem> captor = ArgumentCaptor.forClass(NewBeeMallShoppingCartItem.class);
        verify(shoppingCartItemMapper).updateByPrimaryKeySelective(captor.capture());
        assertEquals(3, captor.getValue().getGoodsCount().intValue());
    }

    @Test
    void saveNewBeeMallCartItem_whenGoodsMissing_returnsGoodsNotExist() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(1L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(null);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_whenSingleGoodsCountExceedsLimit_returnsLimitMessage() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(1L, 1L, 10L, Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER + 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(0);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
    }

    @Test
    void saveNewBeeMallCartItem_whenTotalCartItemsExceedLimit_returnsTotalLimitMessage() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(1L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 10L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER);

        String result = shoppingCartService.saveNewBeeMallCartItem(item);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_TOTAL_NUMBER_ERROR.getResult(), result);
    }

    @Test
    void updateNewBeeMallCartItem_withExistingOwnedItemAndNewQuantity_returnsSuccess() {
        NewBeeMallShoppingCartItem request = ServiceTestFixtures.cartItem(100L, 1L, 10L, 4);
        NewBeeMallShoppingCartItem existing = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(existing);
        when(shoppingCartItemMapper.updateByPrimaryKeySelective(existing)).thenReturn(1);

        String result = shoppingCartService.updateNewBeeMallCartItem(request);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(4, existing.getGoodsCount().intValue());
    }

    @Test
    void updateNewBeeMallCartItem_whenItemMissing_returnsDataNotExist() {
        NewBeeMallShoppingCartItem request = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(null);

        String result = shoppingCartService.updateNewBeeMallCartItem(request);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateNewBeeMallCartItem_whenQuantityExceedsLimit_returnsLimitMessage() {
        NewBeeMallShoppingCartItem request = ServiceTestFixtures.cartItem(100L, 1L, 10L, Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER + 1);
        NewBeeMallShoppingCartItem existing = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(existing);

        String result = shoppingCartService.updateNewBeeMallCartItem(request);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
    }

    @Test
    void updateNewBeeMallCartItem_whenItemOwnedByDifferentUser_returnsNoPermission() {
        NewBeeMallShoppingCartItem request = ServiceTestFixtures.cartItem(100L, 1L, 10L, 2);
        NewBeeMallShoppingCartItem existing = ServiceTestFixtures.cartItem(100L, 2L, 10L, 1);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(existing);

        String result = shoppingCartService.updateNewBeeMallCartItem(request);

        assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), result);
    }

    @Test
    void deleteById_withOwnedItem_returnsTrue() {
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(ServiceTestFixtures.cartItem(100L, 1L, 10L, 1));
        when(shoppingCartItemMapper.deleteByPrimaryKey(100L)).thenReturn(1);

        Boolean result = shoppingCartService.deleteById(100L, 1L);

        assertTrue(result);
    }

    @Test
    void deleteById_whenItemMissing_returnsFalse() {
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(null);

        Boolean result = shoppingCartService.deleteById(100L, 1L);

        assertFalse(result);
    }

    @Test
    void deleteById_whenItemOwnedByDifferentUser_returnsFalse() {
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(ServiceTestFixtures.cartItem(100L, 2L, 10L, 1));

        Boolean result = shoppingCartService.deleteById(100L, 1L);

        assertFalse(result);
    }

    @Test
    void getNewBeeMallCartItemById_returnsMapperResult() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(100L, 1L, 10L, 1);
        when(shoppingCartItemMapper.selectByPrimaryKey(100L)).thenReturn(item);

        NewBeeMallShoppingCartItem result = shoppingCartService.getNewBeeMallCartItemById(100L);

        assertSame(item, result);
    }

    @Test
    void getMyShoppingCartItems_returnsCartVosAndTruncatesLongGoodsName() {
        NewBeeMallShoppingCartItem item = ServiceTestFixtures.cartItem(100L, 1L, 10L, 2);
        NewBeeMallGoods goods = ServiceTestFixtures.goods(10L, 3L, ServiceTestFixtures.repeat('g', 29));
        goods.setSellingPrice(99);
        when(shoppingCartItemMapper.selectByUserId(1L, Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER)).thenReturn(Collections.singletonList(item));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertEquals(1, result.size());
        assertEquals(ServiceTestFixtures.repeat('g', 28) + "...", result.get(0).getGoodsName());
        assertEquals(99, result.get(0).getSellingPrice().intValue());
        assertEquals(2, result.get(0).getGoodsCount().intValue());
    }

    @Test
    void getMyShoppingCartItems_whenCartIsEmpty_returnsEmptyList() {
        when(shoppingCartItemMapper.selectByUserId(1L, Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER)).thenReturn(Collections.<NewBeeMallShoppingCartItem>emptyList());

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertTrue(result.isEmpty());
    }
}
