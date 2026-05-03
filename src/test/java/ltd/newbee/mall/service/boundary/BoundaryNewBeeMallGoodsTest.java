package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.service.impl.NewBeeMallGoodsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoundaryNewBeeMallGoodsTest {

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @InjectMocks
    private NewBeeMallGoodsServiceImpl goodsService;

    @Test
    void getNewBeeMallGoodsById_whenGoodsIsUp_returnsGoodsForDisplay() {
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setGoodsSellStatus((byte) Constants.SELL_STATUS_UP);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(goods);

        NewBeeMallGoods result = goodsService.getNewBeeMallGoodsById(10L);

        assertSame(goods, result);
        assertEquals(Constants.SELL_STATUS_UP, result.getGoodsSellStatus().intValue());
    }
}
