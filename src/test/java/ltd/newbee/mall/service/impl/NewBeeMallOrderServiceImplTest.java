package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.NewBeeMallOrderStatusEnum;
import ltd.newbee.mall.common.PayStatusEnum;
import ltd.newbee.mall.common.PayTypeEnum;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallOrderDetailVO;
import ltd.newbee.mall.controller.vo.NewBeeMallOrderItemVO;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.controller.vo.NewBeeMallUserVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderItemMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallOrder;
import ltd.newbee.mall.entity.NewBeeMallOrderItem;
import ltd.newbee.mall.entity.StockNumDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallOrderServiceImplTest {

    @Mock
    private NewBeeMallOrderMapper orderMapper;

    @Mock
    private NewBeeMallOrderItemMapper orderItemMapper;

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallOrderServiceImpl orderService;

    @Test
    void saveOrder_whenCartGoodsAreValid_deletesCartUpdatesStockAndSavesOrderSnapshots() {
        NewBeeMallUserVO user = ServiceTestFixtures.userVO(1L);
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 2, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
        when(orderMapper.insertSelective(any(NewBeeMallOrder.class))).thenAnswer(invocation -> {
            NewBeeMallOrder order = invocation.getArgument(0);
            order.setOrderId(500L);
            return 1;
        });
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);

        String orderNo = orderService.saveOrder(user, cartItems);

        assertNotNull(orderNo);
        verify(shoppingCartItemMapper).deleteBatch(Collections.singletonList(100L));
        verify(goodsMapper).updateStockNum(anyList());
        ArgumentCaptor<NewBeeMallOrder> orderCaptor = ArgumentCaptor.forClass(NewBeeMallOrder.class);
        verify(orderMapper).insertSelective(orderCaptor.capture());
        assertEquals(1L, orderCaptor.getValue().getUserId());
        assertEquals("address", orderCaptor.getValue().getUserAddress());
        assertEquals(40, orderCaptor.getValue().getTotalPrice().intValue());
        ArgumentCaptor<List> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemMapper).insertBatch(orderItemsCaptor.capture());
        List<?> savedOrderItems = orderItemsCaptor.getValue();
        assertEquals(1, savedOrderItems.size());
        assertEquals(500L, ((NewBeeMallOrderItem) savedOrderItems.get(0)).getOrderId());
    }

    @Test
    void saveOrder_whenCartItemsEmpty_throwsShoppingCartDataError() {
        when(goodsMapper.selectByPrimaryKeys(Collections.<Long>emptyList())).thenReturn(Collections.<NewBeeMallGoods>emptyList());

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), Collections.<NewBeeMallShoppingCartItemVO>emptyList()));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenGoodsMissing_throwsShoppingCartDataError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.<NewBeeMallGoods>emptyList());

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenGoodsIsPutDown_throwsPutDownMessage() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        NewBeeMallGoods goods = ServiceTestFixtures.goods(10L, 3L, "put-down-goods");
        goods.setGoodsSellStatus((byte) Constants.SELL_STATUS_DOWN);
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals("put-down-goods已下架，无法生成订单", exception.getMessage());
    }

    @Test
    void saveOrder_whenPurchaseCountGreaterThanStock_throwsStockError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 11, 20));
        NewBeeMallGoods goods = ServiceTestFixtures.goods(10L, 3L, "goods");
        goods.setStockNum(10);
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenTotalPriceIsZero_throwsOrderPriceError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 0));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.ORDER_PRICE_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenDeleteCartItemsFails_throwsDatabaseError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenStockUpdateFails_throwsStockError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenOrderSaveFails_throwsDatabaseError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
        when(orderMapper.insertSelective(any(NewBeeMallOrder.class))).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenOrderItemsSaveFails_throwsOrderGenerateError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(ServiceTestFixtures.cartItemVO(100L, 10L, 1, 20));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(ServiceTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
        when(orderMapper.insertSelective(any(NewBeeMallOrder.class))).thenAnswer(invocation -> {
            NewBeeMallOrder order = invocation.getArgument(0);
            order.setOrderId(500L);
            return 1;
        });
        when(orderItemMapper.insertBatch(anyList())).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(ServiceTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.ORDER_GENERATE_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void paySuccess_withPrePayOrder_updatesPayAndOrderStatus() {
        NewBeeMallOrder order = ServiceTestFixtures.prePayOrder(1L, "NO1", 1L);
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(order)).thenReturn(1);

        String result = orderService.paySuccess("NO1", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus(), order.getOrderStatus().intValue());
        assertEquals(PayTypeEnum.ALI_PAY.getPayType(), order.getPayType().intValue());
        assertEquals(PayStatusEnum.PAY_SUCCESS.getPayStatus(), order.getPayStatus().intValue());
    }

    @Test
    void paySuccess_whenOrderMissing_returnsOrderNotExist() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(null);

        String result = orderService.paySuccess("NO1", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
    }

    @Test
    void paySuccess_whenOrderIsNotPrePay_returnsOrderStatusError() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus()));

        String result = orderService.paySuccess("NO1", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
    }

    @Test
    void checkDone_withPaidOrders_returnsSuccess() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus())));
        when(orderMapper.checkDone(Collections.singletonList(1L))).thenReturn(1);

        String result = orderService.checkDone(ids);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void checkDone_whenOrderIsNotPaid_returnsOrderStatusMessage() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L)));

        String result = orderService.checkDone(ids);

        assertTrue(result.contains("NO1"));
        assertTrue(result.contains("支付成功"));
    }

    @Test
    void checkOut_withPaidOrder_returnsSuccess() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus())));
        when(orderMapper.checkOut(Collections.singletonList(1L))).thenReturn(1);

        String result = orderService.checkOut(ids);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void checkOut_withPackagedOrder_returnsSuccess() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PACKAGED.getOrderStatus())));
        when(orderMapper.checkOut(Collections.singletonList(1L))).thenReturn(1);

        String result = orderService.checkOut(ids);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void checkOut_whenOrderStatusInvalid_returnsOrderStatusMessage() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L)));

        String result = orderService.checkOut(ids);

        assertTrue(result.contains("NO1"));
        assertTrue(result.contains("支付成功或配货完成"));
    }

    @Test
    void closeOrder_withClosableOrder_recoversStockAndReturnsSuccess() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L)));
        when(orderMapper.closeOrder(Collections.singletonList(1L), NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus())).thenReturn(1);
        when(orderItemMapper.selectByOrderIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

        String result = orderService.closeOrder(ids);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void closeOrder_whenOrderAlreadyCompleted_returnsCannotCloseMessage() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus())));

        String result = orderService.closeOrder(ids);

        assertTrue(result.contains("NO1"));
        assertTrue(result.contains("不能执行关闭操作"));
    }

    @Test
    void closeOrder_whenRecoverStockFails_throwsCloseOrderError() {
        Long[] ids = new Long[]{1L};
        when(orderMapper.selectByPrimaryKeys(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L)));
        when(orderMapper.closeOrder(Collections.singletonList(1L), NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus())).thenReturn(1);
        when(orderItemMapper.selectByOrderIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.closeOrder(ids));

        assertEquals(ServiceResultEnum.CLOSE_ORDER_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void cancelOrder_withOwnCancelableOrder_closesOrderAndRecoversStock() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L));
        when(orderMapper.closeOrder(Collections.singletonList(1L), NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus())).thenReturn(1);
        when(orderItemMapper.selectByOrderIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

        String result = orderService.cancelOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void cancelOrder_whenOrderMissing_returnsOrderNotExist() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(null);

        String result = orderService.cancelOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
    }

    @Test
    void cancelOrder_whenOrderBelongsToDifferentUser_throwsNoPermission() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.prePayOrder(1L, "NO1", 2L));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.cancelOrder("NO1", 1L));

        assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void cancelOrder_whenOrderCannotBeCanceled_returnsOrderStatusError() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus()));

        String result = orderService.cancelOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
    }

    @Test
    void finishOrder_withExpressOrder_updatesToSuccess() {
        NewBeeMallOrder order = ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(order)).thenReturn(1);

        String result = orderService.finishOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus(), order.getOrderStatus().intValue());
    }

    @Test
    void finishOrder_whenOrderIsNotExpress_returnsOrderStatusError() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus()));

        String result = orderService.finishOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
    }

    @Test
    void finishOrder_whenOrderBelongsToDifferentUser_returnsNoPermission() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.order(1L, "NO1", 2L, NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus()));

        String result = orderService.finishOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), result);
    }

    @Test
    void getOrderDetailByOrderNo_withOwnOrderAndItems_returnsDetailVo() {
        NewBeeMallOrder order = ServiceTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
        order.setPayType((byte) PayTypeEnum.ALI_PAY.getPayType());
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));

        NewBeeMallOrderDetailVO result = orderService.getOrderDetailByOrderNo("NO1", 1L);

        assertEquals("NO1", result.getOrderNo());
        assertEquals("已支付", result.getOrderStatusString());
        assertEquals("支付宝", result.getPayTypeString());
        assertEquals(1, result.getNewBeeMallOrderItemVOS().size());
    }

    @Test
    void getOrderDetailByOrderNo_whenOrderMissing_throwsOrderNotExist() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(null);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.getOrderDetailByOrderNo("NO1", 1L));

        assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void getOrderDetailByOrderNo_whenOrderBelongsToDifferentUser_throwsNoPermission() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.prePayOrder(1L, "NO1", 2L));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.getOrderDetailByOrderNo("NO1", 1L));

        assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void getOrderDetailByOrderNo_whenOrderItemsEmpty_throwsOrderItemNotExist() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L));
        when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.<NewBeeMallOrderItem>emptyList());

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.getOrderDetailByOrderNo("NO1", 1L));

        assertEquals(ServiceResultEnum.ORDER_ITEM_NOT_EXIST_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void getOrderItems_withExistingOrderAndItems_returnsOrderItemVos() {
        when(orderMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.prePayOrder(1L, "NO1", 1L));
        when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));

        List<NewBeeMallOrderItemVO> result = orderService.getOrderItems(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getGoodsId());
    }

    @Test
    void getOrderItems_whenOrderMissing_returnsNull() {
        when(orderMapper.selectByPrimaryKey(1L)).thenReturn(null);

        List<NewBeeMallOrderItemVO> result = orderService.getOrderItems(1L);

        assertEquals(null, result);
    }

    @Test
    void recoverStockNum_whenMapperUpdatesRows_returnsTrue() {
        when(orderItemMapper.selectByOrderIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

        Boolean result = orderService.recoverStockNum(Collections.singletonList(1L));

        assertTrue(result);
        ArgumentCaptor<List> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(goodsMapper).recoverStockNum(stockCaptor.capture());
        assertFalse(stockCaptor.getValue().isEmpty());
        assertSame(StockNumDTO.class, stockCaptor.getValue().get(0).getClass());
    }

    @Test
    void recoverStockNum_whenMapperUpdatesNoRows_throwsCloseOrderError() {
        when(orderItemMapper.selectByOrderIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(ServiceTestFixtures.orderItem(1L, 1L, 10L, 2)));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(0);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.recoverStockNum(Collections.singletonList(1L)));

        assertEquals(ServiceResultEnum.CLOSE_ORDER_ERROR.getResult(), exception.getMessage());
    }
}
