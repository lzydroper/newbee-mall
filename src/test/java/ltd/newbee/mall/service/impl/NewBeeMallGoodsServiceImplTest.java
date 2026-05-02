package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.NewBeeMallCategoryLevelEnum;
import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallSearchGoodsVO;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.GoodsCategory;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.util.PageQueryUtil;
import ltd.newbee.mall.util.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallGoodsServiceImplTest {

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @InjectMocks
    private NewBeeMallGoodsServiceImpl goodsService;

    @Test
    void saveNewBeeMallGoods_withValidGoods_returnsSuccess() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "goods");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.thirdLevelCategory(3L));
        when(goodsMapper.selectByCategoryIdAndName("goods", 3L)).thenReturn(null);
        when(goodsMapper.insertSelective(goods)).thenReturn(1);

        String result = goodsService.saveNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveNewBeeMallGoods_whenCategoryMissing_returnsCategoryError() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "goods");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(null);

        String result = goodsService.saveNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
    }

    @Test
    void saveNewBeeMallGoods_whenCategoryIsNotThirdLevel_returnsCategoryError() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 2L, "goods");
        GoodsCategory secondLevel = ServiceTestFixtures.category(2L, NewBeeMallCategoryLevelEnum.LEVEL_TWO.getLevel(), 1L, "second");
        when(goodsCategoryMapper.selectByPrimaryKey(2L)).thenReturn(secondLevel);

        String result = goodsService.saveNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
    }

    @Test
    void saveNewBeeMallGoods_whenSameNameExistsInCategory_returnsSameGoodsMessage() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "goods");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.thirdLevelCategory(3L));
        when(goodsMapper.selectByCategoryIdAndName("goods", 3L)).thenReturn(ServiceTestFixtures.goods(9L, 3L, "goods"));

        String result = goodsService.saveNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.SAME_GOODS_EXIST.getResult(), result);
    }

    @Test
    void updateNewBeeMallGoods_withValidGoods_returnsSuccess() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "newName");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.thirdLevelCategory(3L));
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.goods(1L, 3L, "oldName"));
        when(goodsMapper.selectByCategoryIdAndName("newName", 3L)).thenReturn(null);
        when(goodsMapper.updateByPrimaryKeySelective(goods)).thenReturn(1);

        String result = goodsService.updateNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void updateNewBeeMallGoods_whenGoodsMissing_returnsDataNotExist() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "goods");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.thirdLevelCategory(3L));
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(null);

        String result = goodsService.updateNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateNewBeeMallGoods_whenNewNameBelongsToOtherGoods_returnsSameGoodsMessage() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "newName");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.thirdLevelCategory(3L));
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.goods(1L, 3L, "oldName"));
        when(goodsMapper.selectByCategoryIdAndName("newName", 3L)).thenReturn(ServiceTestFixtures.goods(2L, 3L, "newName"));

        String result = goodsService.updateNewBeeMallGoods(goods);

        assertEquals(ServiceResultEnum.SAME_GOODS_EXIST.getResult(), result);
    }

    @Test
    void batchUpdateSellStatus_withIdsAndValidStatus_returnsTrue() {
        Long[] ids = new Long[]{1L, 2L};
        when(goodsMapper.batchUpdateSellStatus(ids, 0)).thenReturn(2);

        Boolean result = goodsService.batchUpdateSellStatus(ids, 0);

        assertTrue(result);
    }

    @Test
    void batchUpdateSellStatus_withEmptyIds_returnsFalse() {
        when(goodsMapper.batchUpdateSellStatus(any(Long[].class), eq(0))).thenReturn(0);

        Boolean result = goodsService.batchUpdateSellStatus(new Long[]{}, 0);

        assertFalse(result);
    }

    @Test
    void getNewBeeMallGoodsById_whenGoodsExists_returnsGoods() {
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, "goods");
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(goods);

        NewBeeMallGoods result = goodsService.getNewBeeMallGoodsById(1L);

        assertSame(goods, result);
    }

    @Test
    void getNewBeeMallGoodsById_whenGoodsMissing_throwsGoodsNotExist() {
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(null);

        NewBeeMallException exception = assertThrows(NewBeeMallException.class, () -> goodsService.getNewBeeMallGoodsById(1L));

        assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), exception.getMessage());
    }

    @Test
    void searchNewBeeMallGoods_returnsPageAndTruncatesLongNameAndIntro() {
        PageQueryUtil pageQuery = ServiceTestFixtures.pageQuery();
        NewBeeMallGoods goods = ServiceTestFixtures.goods(1L, 3L, ServiceTestFixtures.repeat('n', 29));
        goods.setGoodsIntro(ServiceTestFixtures.repeat('i', 31));
        when(goodsMapper.findNewBeeMallGoodsListBySearch(pageQuery)).thenReturn(Collections.singletonList(goods));
        when(goodsMapper.getTotalNewBeeMallGoodsBySearch(pageQuery)).thenReturn(1);

        PageResult result = goodsService.searchNewBeeMallGoods(pageQuery);

        assertEquals(1, result.getTotalCount());
        List<?> list = result.getList();
        assertEquals(1, list.size());
        NewBeeMallSearchGoodsVO goodsVO = (NewBeeMallSearchGoodsVO) list.get(0);
        assertEquals(ServiceTestFixtures.repeat('n', 28) + "...", goodsVO.getGoodsName());
        assertEquals(ServiceTestFixtures.repeat('i', 30) + "...", goodsVO.getGoodsIntro());
    }

    @Test
    void getNewBeeMallGoodsPage_returnsGoodsPage() {
        PageQueryUtil pageQuery = ServiceTestFixtures.pageQuery();
        List<NewBeeMallGoods> goodsList = Arrays.asList(ServiceTestFixtures.goods(1L, 3L, "goods"));
        when(goodsMapper.findNewBeeMallGoodsList(pageQuery)).thenReturn(goodsList);
        when(goodsMapper.getTotalNewBeeMallGoods(pageQuery)).thenReturn(1);

        PageResult result = goodsService.getNewBeeMallGoodsPage(pageQuery);

        assertEquals(goodsList, result.getList());
        assertEquals(1, result.getTotalCount());
        verify(goodsMapper).findNewBeeMallGoodsList(pageQuery);
    }
}
