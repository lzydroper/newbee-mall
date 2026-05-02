package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallIndexConfigGoodsVO;
import ltd.newbee.mall.dao.IndexConfigMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.IndexConfig;
import ltd.newbee.mall.entity.NewBeeMallGoods;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewBeeMallIndexConfigServiceImplTest {

    @Mock
    private IndexConfigMapper indexConfigMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallIndexConfigServiceImpl indexConfigService;

    @Test
    void saveIndexConfig_withExistingGoodsAndUniqueConfig_returnsSuccess() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(null);
        when(indexConfigMapper.insertSelective(indexConfig)).thenReturn(1);

        String result = indexConfigService.saveIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void saveIndexConfig_whenGoodsMissing_returnsGoodsNotExist() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(null);

        String result = indexConfigService.saveIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), result);
    }

    @Test
    void saveIndexConfig_whenSameTypeAndGoodsAlreadyConfigured_returnsSameConfigMessage() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(ServiceTestFixtures.indexConfig(9L, 10L));

        String result = indexConfigService.saveIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.SAME_INDEX_CONFIG_EXIST.getResult(), result);
    }

    @Test
    void saveIndexConfig_whenInsertFails_returnsDatabaseError() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(null);
        when(indexConfigMapper.insertSelective(indexConfig)).thenReturn(0);

        String result = indexConfigService.saveIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    @Test
    void updateIndexConfig_withExistingConfigAndUniqueGoods_returnsSuccess() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.indexConfig(1L, 9L));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(null);
        when(indexConfigMapper.updateByPrimaryKeySelective(indexConfig)).thenReturn(1);

        String result = indexConfigService.updateIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    void updateIndexConfig_whenGoodsMissing_returnsGoodsNotExist() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(null);

        String result = indexConfigService.updateIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateIndexConfig_whenConfigMissing_returnsDataNotExist() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByPrimaryKey(1L)).thenReturn(null);

        String result = indexConfigService.updateIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    void updateIndexConfig_whenSameTypeAndGoodsBelongsToOtherConfig_returnsSameConfigMessage() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.indexConfig(1L, 9L));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(ServiceTestFixtures.indexConfig(2L, 10L));

        String result = indexConfigService.updateIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.SAME_INDEX_CONFIG_EXIST.getResult(), result);
    }

    @Test
    void updateIndexConfig_whenUpdateFails_returnsDatabaseError() {
        IndexConfig indexConfig = ServiceTestFixtures.indexConfig(1L, 10L);
        when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(ServiceTestFixtures.goods(10L, 3L, "goods"));
        when(indexConfigMapper.selectByPrimaryKey(1L)).thenReturn(ServiceTestFixtures.indexConfig(1L, 9L));
        when(indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), 10L)).thenReturn(null);
        when(indexConfigMapper.updateByPrimaryKeySelective(indexConfig)).thenReturn(0);

        String result = indexConfigService.updateIndexConfig(indexConfig);

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    @Test
    void deleteBatch_withIds_returnsTrue() {
        Long[] ids = new Long[]{1L, 2L};
        when(indexConfigMapper.deleteBatch(ids)).thenReturn(2);

        Boolean result = indexConfigService.deleteBatch(ids);

        assertTrue(result);
    }

    @Test
    void deleteBatch_withEmptyIds_returnsFalse() {
        Boolean result = indexConfigService.deleteBatch(new Long[]{});

        assertFalse(result);
    }

    @Test
    void getConfigGoodsesForIndex_returnsGoodsVosAndTruncatesLongFields() {
        IndexConfig config = ServiceTestFixtures.indexConfig(1L, 10L);
        NewBeeMallGoods longGoods = ServiceTestFixtures.goods(10L, 3L, ServiceTestFixtures.repeat('n', 31));
        longGoods.setGoodsIntro(ServiceTestFixtures.repeat('i', 23));
        NewBeeMallGoods boundaryGoods = ServiceTestFixtures.goods(11L, 3L, ServiceTestFixtures.repeat('b', 30));
        boundaryGoods.setGoodsIntro(ServiceTestFixtures.repeat('c', 22));
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(config.getConfigType(), 2)).thenReturn(Arrays.asList(config, ServiceTestFixtures.indexConfig(2L, 11L)));
        when(goodsMapper.selectByPrimaryKeys(Arrays.asList(10L, 11L))).thenReturn(Arrays.asList(longGoods, boundaryGoods));

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(config.getConfigType(), 2);

        assertEquals(2, result.size());
        assertEquals(ServiceTestFixtures.repeat('n', 30) + "...", result.get(0).getGoodsName());
        assertEquals(ServiceTestFixtures.repeat('i', 22) + "...", result.get(0).getGoodsIntro());
        assertEquals(ServiceTestFixtures.repeat('b', 30), result.get(1).getGoodsName());
        assertEquals(ServiceTestFixtures.repeat('c', 22), result.get(1).getGoodsIntro());
    }

    @Test
    void getConfigGoodsesForIndex_whenConfigsEmpty_returnsEmptyList() {
        when(indexConfigMapper.findIndexConfigsByTypeAndNum(3, 2)).thenReturn(Collections.<IndexConfig>emptyList());

        List<NewBeeMallIndexConfigGoodsVO> result = indexConfigService.getConfigGoodsesForIndex(3, 2);

        assertTrue(result.isEmpty());
    }
}
