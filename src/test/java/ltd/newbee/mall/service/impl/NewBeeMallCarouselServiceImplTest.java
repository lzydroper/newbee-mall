package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallIndexCarouselVO;
import ltd.newbee.mall.dao.CarouselMapper;
import ltd.newbee.mall.entity.Carousel;
import ltd.newbee.mall.util.PageQueryUtil;
import ltd.newbee.mall.util.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallCarouselServiceImplTest {

    @Mock
    private CarouselMapper carouselMapper;

    @InjectMocks
    private NewBeeMallCarouselServiceImpl carouselService;

    @Test
    void saveCarousel_whenInsertSucceeds_returnsSuccess() {
        Carousel carousel = ServiceTestFixtures.carousel(1);
        when(carouselMapper.insertSelective(carousel)).thenReturn(1);

        String result = carouselService.saveCarousel(carousel);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveCarousel_whenInsertFails_returnsDatabaseError() {
        Carousel carousel = ServiceTestFixtures.carousel(1);
        when(carouselMapper.insertSelective(carousel)).thenReturn(0);

        String result = carouselService.saveCarousel(carousel);

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    @Test
    void updateCarousel_withExistingCarousel_returnsSuccess() {
        Carousel update = ServiceTestFixtures.carousel(1);
        update.setCarouselUrl("/new.png");
        update.setRedirectUrl("/new");
        update.setCarouselRank(2);
        Carousel existing = ServiceTestFixtures.carousel(1);
        when(carouselMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(carouselMapper.updateByPrimaryKeySelective(existing)).thenReturn(1);

        String result = carouselService.updateCarousel(update);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals("/new.png", existing.getCarouselUrl());
        assertEquals("/new", existing.getRedirectUrl());
        assertEquals(2, existing.getCarouselRank().intValue());
    }

    @Test
    void updateCarousel_whenCarouselMissing_returnsDataNotExist() {
        Carousel update = ServiceTestFixtures.carousel(1);
        when(carouselMapper.selectByPrimaryKey(1)).thenReturn(null);

        String result = carouselService.updateCarousel(update);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateCarousel_whenUpdateFails_returnsDatabaseError() {
        Carousel update = ServiceTestFixtures.carousel(1);
        Carousel existing = ServiceTestFixtures.carousel(1);
        when(carouselMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(carouselMapper.updateByPrimaryKeySelective(existing)).thenReturn(0);

        String result = carouselService.updateCarousel(update);

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    @Test
    void deleteBatch_withIds_returnsTrue() {
        Integer[] ids = new Integer[]{1, 2};
        when(carouselMapper.deleteBatch(ids)).thenReturn(2);

        Boolean result = carouselService.deleteBatch(ids);

        assertTrue(result);
    }

    @Test
    void deleteBatch_withEmptyIds_returnsFalse() {
        Boolean result = carouselService.deleteBatch(new Integer[]{});

        assertFalse(result);
    }

    @Test
    void deleteBatch_whenDeleteFails_returnsFalse() {
        Integer[] ids = new Integer[]{1};
        when(carouselMapper.deleteBatch(ids)).thenReturn(0);

        Boolean result = carouselService.deleteBatch(ids);

        assertFalse(result);
    }

    @Test
    void getCarouselById_returnsMapperResult() {
        Carousel carousel = ServiceTestFixtures.carousel(1);
        when(carouselMapper.selectByPrimaryKey(1)).thenReturn(carousel);

        Carousel result = carouselService.getCarouselById(1);

        assertSame(carousel, result);
    }

    @Test
    void getCarouselsForIndex_returnsCarouselVos() {
        when(carouselMapper.findCarouselsByNum(2)).thenReturn(Collections.singletonList(ServiceTestFixtures.carousel(1)));

        List<NewBeeMallIndexCarouselVO> result = carouselService.getCarouselsForIndex(2);

        assertEquals(1, result.size());
        assertEquals("/carousel.png", result.get(0).getCarouselUrl());
        assertEquals("/redirect", result.get(0).getRedirectUrl());
    }

    @Test
    void getCarouselPage_returnsPageResult() {
        PageQueryUtil pageQuery = ServiceTestFixtures.pageQuery();
        List<Carousel> carousels = Collections.singletonList(ServiceTestFixtures.carousel(1));
        when(carouselMapper.findCarouselList(pageQuery)).thenReturn(carousels);
        when(carouselMapper.getTotalCarousels(pageQuery)).thenReturn(1);

        PageResult result = carouselService.getCarouselPage(pageQuery);

        assertEquals(carousels, result.getList());
        assertEquals(1, result.getTotalCount());
    }
}
