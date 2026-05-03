package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.NewBeeMallOrderStatusEnum;
import ltd.newbee.mall.common.PayTypeEnum;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallOrderDetailVO;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderItemMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallOrder;
import ltd.newbee.mall.entity.NewBeeMallOrderItem;
import ltd.newbee.mall.service.impl.NewBeeMallOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoundaryNewBeeMallOrderTest {

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
    void saveOrder_whenGoodsIsUp_allowsOrder() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 1, 20));
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setGoodsSellStatus((byte) Constants.SELL_STATUS_UP);
        stubSuccessfulOrderCreation(cartItems, goods);

        String orderNo = orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems);

        assertNotNull(orderNo);
    }

    @Test
    void saveOrder_whenGoodsIsDown_rejectsOrder() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 1, 20));
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "put-down-goods");
        goods.setGoodsSellStatus((byte) Constants.SELL_STATUS_DOWN);
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems));

        assertEquals("put-down-goods已下架，无法生成订单", exception.getMessage());
    }

    @Test
    void saveOrder_whenStockEqualsPurchaseCount_allowsOrder() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 5, 20));
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setStockNum(5);
        stubSuccessfulOrderCreation(cartItems, goods);

        String orderNo = orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems);

        assertNotNull(orderNo);
    }

    @Test
    void saveOrder_whenStockIsLessThanPurchaseCount_throwsStockError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 6, 20));
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setStockNum(5);
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenTotalPriceIsOne_allowsOrder() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 1, 1));
        stubSuccessfulOrderCreation(cartItems, BoundaryTestFixtures.goods(10L, 3L, "goods"));

        String orderNo = orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems);

        ArgumentCaptor<NewBeeMallOrder> orderCaptor = ArgumentCaptor.forClass(NewBeeMallOrder.class);
        verify(orderMapper).insertSelective(orderCaptor.capture());
        assertNotNull(orderNo);
        assertEquals(1, orderCaptor.getValue().getTotalPrice().intValue());
    }

    @Test
    void saveOrder_whenTotalPriceIsZero_throwsOrderPriceError() {
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(BoundaryTestFixtures.cartItemVO(100L, 10L, 1, 0));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(BoundaryTestFixtures.goods(10L, 3L, "goods")));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(100L))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(BoundaryTestFixtures.userVO(1L), cartItems));

        assertEquals(ServiceResultEnum.ORDER_PRICE_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void saveOrder_whenCartItemsEmpty_throwsShoppingCartDataError() {
        when(goodsMapper.selectByPrimaryKeys(Collections.<Long>emptyList())).thenReturn(Collections.<NewBeeMallGoods>emptyList());

        NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                () -> orderService.saveOrder(BoundaryTestFixtures.userVO(1L), Collections.<NewBeeMallShoppingCartItemVO>emptyList()));

        assertEquals(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult(), exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = NewBeeMallOrderStatusEnum.class, names = "DEFAULT", mode = EnumSource.Mode.EXCLUDE)
    void getOrderDetailByOrderNo_mapsEveryOrderStatusToDisplayName(NewBeeMallOrderStatusEnum orderStatus) {
        NewBeeMallOrder order = BoundaryTestFixtures.order(1L, "NO1", 1L, orderStatus.getOrderStatus());
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.singletonList(BoundaryTestFixtures.orderItem(1L, 1L, 10L, 2)));

        NewBeeMallOrderDetailVO result = orderService.getOrderDetailByOrderNo("NO1", 1L);

        assertEquals(orderStatus.getName(), result.getOrderStatusString());
    }

    @Test
    void getOrderDetailByOrderNo_whenOrderItemsEmpty_throwsOrderItemNotExist() {
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(BoundaryTestFixtures.prePayOrder(1L, "NO1", 1L));
        when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.<NewBeeMallOrderItem>emptyList());

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> orderService.getOrderDetailByOrderNo("NO1", 1L));

        assertEquals(ServiceResultEnum.ORDER_ITEM_NOT_EXIST_ERROR.getResult(), exception.getMessage());
    }

    @Test
    void paySuccess_whenOrderIsPrePay_updatesStatusToPaid() {
        NewBeeMallOrder order = BoundaryTestFixtures.prePayOrder(1L, "NO1", 1L);
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(order)).thenReturn(1);

        String result = orderService.paySuccess("NO1", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus(), order.getOrderStatus().intValue());
    }

    @Test
    void finishOrder_whenOrderIsExpress_updatesStatusToSuccess() {
        NewBeeMallOrder order = BoundaryTestFixtures.order(1L, "NO1", 1L, NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
        when(orderMapper.selectByOrderNo("NO1")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(order)).thenReturn(1);

        String result = orderService.finishOrder("NO1", 1L);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus(), order.getOrderStatus().intValue());
    }

    @Test
    void getNewBeeMallOrderStatusEnumByStatus_returnsDefinedClosedStatuses() {
        assertSame(NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER,
                NewBeeMallOrderStatusEnum.getNewBeeMallOrderStatusEnumByStatus(-1));
        assertSame(NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED,
                NewBeeMallOrderStatusEnum.getNewBeeMallOrderStatusEnumByStatus(-2));
        assertSame(NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE,
                NewBeeMallOrderStatusEnum.getNewBeeMallOrderStatusEnumByStatus(-3));
    }

    private void stubSuccessfulOrderCreation(List<NewBeeMallShoppingCartItemVO> cartItems, NewBeeMallGoods goods) {
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(goods.getGoodsId()))).thenReturn(Collections.singletonList(goods));
        when(shoppingCartItemMapper.deleteBatch(Collections.singletonList(cartItems.get(0).getCartItemId()))).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
        when(orderMapper.insertSelective(any(NewBeeMallOrder.class))).thenAnswer(invocation -> {
            NewBeeMallOrder order = invocation.getArgument(0);
            order.setOrderId(500L);
            return 1;
        });
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);
    }
}
