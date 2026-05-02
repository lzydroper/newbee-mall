package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.controller.vo.NewBeeMallIndexConfigGoodsVO;
import ltd.newbee.mall.dao.IndexConfigMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.IndexConfig;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.service.impl.NewBeeMallIndexConfigServiceImpl;
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
class NewBeeMallIndexConfigBoundaryTest {

    @Mock
    private IndexConfigMapper indexConfigMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallIndexConfigServiceImpl indexConfigService;

    @Test
    void getConfigGoodsesForIndex_withNameLengthThirty_doesNotTruncateName() {
        IndexConfig config = BoundaryTestFixtures.indexConfig(1L, 10L);
        String boundaryName = BoundaryTestFixtures.repeat('n', 30);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, boundaryName);
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(config.getConfigType(), 1)).thenReturn(Collections.singletonList(config));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(config.getConfigType(), 1);

        assertEquals(1, result.size());
        assertEquals(boundaryName, result.get(0).getGoodsName());
    }

    @Test
    void getConfigGoodsesForIndex_withNameLengthThirtyOne_truncatesName() {
        IndexConfig config = BoundaryTestFixtures.indexConfig(1L, 10L);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, BoundaryTestFixtures.repeat('n', 31));
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(config.getConfigType(), 1)).thenReturn(Collections.singletonList(config));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(config.getConfigType(), 1);

        assertEquals(1, result.size());
        assertEquals(BoundaryTestFixtures.repeat('n', 30) + "...", result.get(0).getGoodsName());
    }

    @Test
    void getConfigGoodsesForIndex_withIntroLengthTwentyTwo_doesNotTruncateIntro() {
        IndexConfig config = BoundaryTestFixtures.indexConfig(1L, 10L);
        String boundaryIntro = BoundaryTestFixtures.repeat('i', 22);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setGoodsIntro(boundaryIntro);
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(config.getConfigType(), 1)).thenReturn(Collections.singletonList(config));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(config.getConfigType(), 1);

        assertEquals(1, result.size());
        assertEquals(boundaryIntro, result.get(0).getGoodsIntro());
    }

    @Test
    void getConfigGoodsesForIndex_withIntroLengthTwentyThree_truncatesIntro() {
        IndexConfig config = BoundaryTestFixtures.indexConfig(1L, 10L);
        NewBeeMallGoods goods = BoundaryTestFixtures.goods(10L, 3L, "goods");
        goods.setGoodsIntro(BoundaryTestFixtures.repeat('i', 23));
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(config.getConfigType(), 1)).thenReturn(Collections.singletonList(config));
        when(goodsMapper.selectByPrimaryKeys(Collections.singletonList(10L))).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(config.getConfigType(), 1);

        assertEquals(1, result.size());
        assertEquals(BoundaryTestFixtures.repeat('i', 22) + "...", result.get(0).getGoodsIntro());
    }
}
