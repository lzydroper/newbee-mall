package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.NewBeeMallCategoryLevelEnum;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallIndexCategoryVO;
import ltd.newbee.mall.controller.vo.SearchPageCategoryVO;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.entity.GoodsCategory;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallCategoryServiceImplTest {

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @InjectMocks
    private NewBeeMallCategoryServiceImpl categoryService;

    @Test
    void saveCategory_withUniqueSameLevelName_returnsSuccess() {
        GoodsCategory category = ServiceTestFixtures.category(3L, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third");
        when(goodsCategoryMapper.selectByLevelAndName(category.getCategoryLevel(), "third")).thenReturn(null);
        when(goodsCategoryMapper.insertSelective(category)).thenReturn(1);

        String result = categoryService.saveCategory(category);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveCategory_whenSameLevelNameExists_returnsSameCategoryMessage() {
        GoodsCategory category = ServiceTestFixtures.category(3L, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third");
        when(goodsCategoryMapper.selectByLevelAndName(category.getCategoryLevel(), "third")).thenReturn(ServiceTestFixtures.category(9L, 3, 8L, "third"));

        String result = categoryService.saveCategory(category);

        assertEquals(ServiceResultEnum.SAME_CATEGORY_EXIST.getResult(), result);
    }

    @Test
    void updateGoodsCategory_withExistingCategoryAndUniqueName_returnsSuccess() {
        GoodsCategory category = ServiceTestFixtures.category(3L, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third-new");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.category(3L, 3, 2L, "third-old"));
        when(goodsCategoryMapper.selectByLevelAndName(category.getCategoryLevel(), "third-new")).thenReturn(null);
        when(goodsCategoryMapper.updateByPrimaryKeySelective(category)).thenReturn(1);

        String result = categoryService.updateGoodsCategory(category);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void updateGoodsCategory_whenCategoryMissing_returnsDataNotExist() {
        GoodsCategory category = ServiceTestFixtures.category(3L, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(null);

        String result = categoryService.updateGoodsCategory(category);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateGoodsCategory_whenSameLevelNameBelongsToOtherCategory_returnsSameCategoryMessage() {
        GoodsCategory category = ServiceTestFixtures.category(3L, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third-new");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(ServiceTestFixtures.category(3L, 3, 2L, "third-old"));
        when(goodsCategoryMapper.selectByLevelAndName(category.getCategoryLevel(), "third-new")).thenReturn(ServiceTestFixtures.category(4L, 3, 2L, "third-new"));

        String result = categoryService.updateGoodsCategory(category);

        assertEquals(ServiceResultEnum.SAME_CATEGORY_EXIST.getResult(), result);
    }

    @Test
    void deleteBatch_withIds_returnsTrue() {
        Integer[] ids = new Integer[]{1, 2};
        when(goodsCategoryMapper.deleteBatch(ids)).thenReturn(2);

        Boolean result = categoryService.deleteBatch(ids);

        assertTrue(result);
    }

    @Test
    void deleteBatch_withEmptyIds_returnsFalse() {
        Boolean result = categoryService.deleteBatch(new Integer[]{});

        assertFalse(result);
    }

    @Test
    void getGoodsCategoryById_returnsMapperResult() {
        GoodsCategory category = ServiceTestFixtures.category(1L, 1, 0L, "first");
        when(goodsCategoryMapper.selectByPrimaryKey(1L)).thenReturn(category);

        GoodsCategory result = categoryService.getGoodsCategoryById(1L);

        assertSame(category, result);
    }

    @Test
    void getCategoriesForIndex_withThreeLevels_returnsNestedCategoryVos() {
        GoodsCategory first = ServiceTestFixtures.category(1L, 1, 0L, "first");
        GoodsCategory second = ServiceTestFixtures.category(2L, 2, 1L, "second");
        GoodsCategory third = ServiceTestFixtures.category(3L, 3, 2L, "third");
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(0L), NewBeeMallCategoryLevelEnum.LEVEL_ONE.getLevel(), Constants.INDEX_CATEGORY_NUMBER))
                .thenReturn(Collections.singletonList(first));
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(1L), NewBeeMallCategoryLevelEnum.LEVEL_TWO.getLevel(), 0))
                .thenReturn(Collections.singletonList(second));
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(2L), NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 0))
                .thenReturn(Collections.singletonList(third));

        List<NewBeeMallIndexCategoryVO> result = categoryService.getCategoriesForIndex();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("first", result.get(0).getCategoryName());
        assertEquals("second", result.get(0).getSecondLevelCategoryVOS().get(0).getCategoryName());
        assertEquals("third", result.get(0).getSecondLevelCategoryVOS().get(0).getThirdLevelCategoryVOS().get(0).getCategoryName());
    }

    @Test
    void getCategoriesForIndex_whenFirstLevelCategoriesEmpty_returnsNull() {
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(0L), NewBeeMallCategoryLevelEnum.LEVEL_ONE.getLevel(), Constants.INDEX_CATEGORY_NUMBER))
                .thenReturn(Collections.<GoodsCategory>emptyList());

        List<NewBeeMallIndexCategoryVO> result = categoryService.getCategoriesForIndex();

        assertNull(result);
    }

    @Test
    void getCategoriesForSearch_withThirdLevelCategory_returnsCurrentAndSiblingCategories() {
        GoodsCategory third = ServiceTestFixtures.category(3L, 3, 2L, "third");
        GoodsCategory second = ServiceTestFixtures.category(2L, 2, 1L, "second");
        GoodsCategory sibling = ServiceTestFixtures.category(4L, 3, 2L, "sibling");
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(third);
        when(goodsCategoryMapper.selectByPrimaryKey(2L)).thenReturn(second);
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(2L), NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), Constants.SEARCH_CATEGORY_NUMBER))
                .thenReturn(Arrays.asList(third, sibling));

        SearchPageCategoryVO result = categoryService.getCategoriesForSearch(3L);

        assertNotNull(result);
        assertEquals("third", result.getCurrentCategoryName());
        assertEquals("second", result.getSecondLevelCategoryName());
        assertEquals(2, result.getThirdLevelCategoryList().size());
    }

    @Test
    void getCategoriesForSearch_whenCategoryMissing_returnsNull() {
        when(goodsCategoryMapper.selectByPrimaryKey(3L)).thenReturn(null);

        SearchPageCategoryVO result = categoryService.getCategoriesForSearch(3L);

        assertNull(result);
    }

    @Test
    void getCategoriesForSearch_whenCategoryIsNotThirdLevel_returnsNull() {
        when(goodsCategoryMapper.selectByPrimaryKey(2L)).thenReturn(ServiceTestFixtures.category(2L, 2, 1L, "second"));

        SearchPageCategoryVO result = categoryService.getCategoriesForSearch(2L);

        assertNull(result);
    }

    @Test
    void selectByLevelAndParentIdsAndNumber_delegatesToMapperWithZeroLimit() {
        List<Long> parentIds = Collections.singletonList(1L);
        List<GoodsCategory> categories = Collections.singletonList(ServiceTestFixtures.category(2L, 2, 1L, "second"));
        when(goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(eq(parentIds), eq(2), eq(0))).thenReturn(categories);

        List<GoodsCategory> result = categoryService.selectByLevelAndParentIdsAndNumber(parentIds, 2);

        assertSame(categories, result);
    }
}
