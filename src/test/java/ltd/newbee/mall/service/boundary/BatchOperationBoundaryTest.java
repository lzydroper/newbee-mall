package ltd.newbee.mall.service.boundary;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.dao.CarouselMapper;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.dao.IndexConfigMapper;
import ltd.newbee.mall.dao.MallUserMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.service.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchOperationBoundaryTest {

    @Mock
    private MallUserMapper mallUserMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @Mock
    private IndexConfigMapper indexConfigMapper;

    @Mock
    private CarouselMapper carouselMapper;

    @InjectMocks
    private NewBeeMallUserServiceImpl userService;

    @InjectMocks
    private NewBeeMallCategoryServiceImpl categoryService;

    @InjectMocks
    private NewBeeMallGoodsServiceImpl goodsService;

    @InjectMocks
    private NewBeeMallIndexConfigServiceImpl indexConfigService;

    @InjectMocks
    private NewBeeMallCarouselServiceImpl carouselService;

    @Test
    void lockUsers_withEmptyIds_returnsFalse() {
        assertFalse(userService.lockUsers(new Integer[]{}, 1));
    }

    @Test
    void deleteCategories_withEmptyIds_returnsFalse() {
        assertFalse(categoryService.deleteBatch(new Integer[]{}));
    }

    @Test
    void batchUpdateSellStatus_withEmptyIds_returnsFalse() {
        when(goodsMapper.batchUpdateSellStatus(any(Long[].class), eq(Constants.SELL_STATUS_UP))).thenReturn(0);

        assertFalse(goodsService.batchUpdateSellStatus(new Long[]{}, Constants.SELL_STATUS_UP));
    }

    @Test
    void deleteIndexConfigs_withEmptyIds_returnsFalse() {
        assertFalse(indexConfigService.deleteBatch(new Long[]{}));
    }

    @Test
    void deleteCarousels_withEmptyIds_returnsFalse() {
        assertFalse(carouselService.deleteBatch(new Integer[]{}));
    }
}
