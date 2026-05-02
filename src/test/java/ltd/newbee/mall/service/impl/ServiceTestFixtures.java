package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.IndexConfigTypeEnum;
import ltd.newbee.mall.common.NewBeeMallCategoryLevelEnum;
import ltd.newbee.mall.common.NewBeeMallOrderStatusEnum;
import ltd.newbee.mall.common.PayStatusEnum;
import ltd.newbee.mall.common.PayTypeEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.controller.vo.NewBeeMallUserVO;
import ltd.newbee.mall.entity.AdminUser;
import ltd.newbee.mall.entity.Carousel;
import ltd.newbee.mall.entity.GoodsCategory;
import ltd.newbee.mall.entity.IndexConfig;
import ltd.newbee.mall.entity.MallUser;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallOrder;
import ltd.newbee.mall.entity.NewBeeMallOrderItem;
import ltd.newbee.mall.entity.NewBeeMallShoppingCartItem;
import ltd.newbee.mall.util.MD5Util;
import ltd.newbee.mall.util.PageQueryUtil;

import java.util.HashMap;
import java.util.Map;

final class ServiceTestFixtures {

    private ServiceTestFixtures() {
    }

    static PageQueryUtil pageQuery() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "10");
        return new PageQueryUtil(params);
    }

    static MallUser mallUser(Long userId, String loginName, String nickName, int lockedFlag) {
        MallUser user = new MallUser();
        user.setUserId(userId);
        user.setLoginName(loginName);
        user.setNickName(nickName);
        user.setPasswordMd5(MD5Util.MD5Encode("password", "UTF-8"));
        user.setAddress("address");
        user.setIntroduceSign("sign");
        user.setLockedFlag((byte) lockedFlag);
        return user;
    }

    static NewBeeMallUserVO userVO(Long userId) {
        NewBeeMallUserVO userVO = new NewBeeMallUserVO();
        userVO.setUserId(userId);
        userVO.setLoginName("mall-user");
        userVO.setNickName("mall-user");
        userVO.setAddress("address");
        userVO.setIntroduceSign("sign");
        return userVO;
    }

    static AdminUser adminUser(Integer adminUserId, String rawPassword) {
        AdminUser adminUser = new AdminUser();
        adminUser.setAdminUserId(adminUserId);
        adminUser.setLoginUserName("admin");
        adminUser.setNickName("administrator");
        adminUser.setLoginPassword(MD5Util.MD5Encode(rawPassword, "UTF-8"));
        return adminUser;
    }

    static GoodsCategory category(Long id, int level, Long parentId, String name) {
        GoodsCategory category = new GoodsCategory();
        category.setCategoryId(id);
        category.setCategoryLevel((byte) level);
        category.setParentId(parentId);
        category.setCategoryName(name);
        category.setCategoryRank(1);
        return category;
    }

    static GoodsCategory thirdLevelCategory(Long id) {
        return category(id, NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel(), 2L, "third");
    }

    static NewBeeMallGoods goods(Long goodsId, Long categoryId, String goodsName) {
        NewBeeMallGoods goods = new NewBeeMallGoods();
        goods.setGoodsId(goodsId);
        goods.setGoodsCategoryId(categoryId);
        goods.setGoodsName(goodsName);
        goods.setGoodsIntro("intro");
        goods.setTag("tag");
        goods.setGoodsCoverImg("/goods.png");
        goods.setSellingPrice(10);
        goods.setStockNum(10);
        goods.setGoodsSellStatus((byte) Constants.SELL_STATUS_UP);
        return goods;
    }

    static NewBeeMallShoppingCartItem cartItem(Long cartItemId, Long userId, Long goodsId, int goodsCount) {
        NewBeeMallShoppingCartItem item = new NewBeeMallShoppingCartItem();
        item.setCartItemId(cartItemId);
        item.setUserId(userId);
        item.setGoodsId(goodsId);
        item.setGoodsCount(goodsCount);
        return item;
    }

    static NewBeeMallShoppingCartItemVO cartItemVO(Long cartItemId, Long goodsId, int goodsCount, int sellingPrice) {
        NewBeeMallShoppingCartItemVO itemVO = new NewBeeMallShoppingCartItemVO();
        itemVO.setCartItemId(cartItemId);
        itemVO.setGoodsId(goodsId);
        itemVO.setGoodsCount(goodsCount);
        itemVO.setGoodsName("goods-" + goodsId);
        itemVO.setGoodsCoverImg("/goods-" + goodsId + ".png");
        itemVO.setSellingPrice(sellingPrice);
        return itemVO;
    }

    static NewBeeMallOrder order(Long orderId, String orderNo, Long userId, int status) {
        NewBeeMallOrder order = new NewBeeMallOrder();
        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalPrice(20);
        order.setPayType((byte) PayTypeEnum.NOT_PAY.getPayType());
        order.setPayStatus((byte) PayStatusEnum.PAY_ING.getPayStatus());
        order.setOrderStatus((byte) status);
        order.setUserAddress("address");
        order.setIsDeleted((byte) 0);
        return order;
    }

    static NewBeeMallOrder prePayOrder(Long orderId, String orderNo, Long userId) {
        return order(orderId, orderNo, userId, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
    }

    static NewBeeMallOrderItem orderItem(Long orderItemId, Long orderId, Long goodsId, int goodsCount) {
        NewBeeMallOrderItem item = new NewBeeMallOrderItem();
        item.setOrderItemId(orderItemId);
        item.setOrderId(orderId);
        item.setGoodsId(goodsId);
        item.setGoodsName("goods-" + goodsId);
        item.setGoodsCoverImg("/goods-" + goodsId + ".png");
        item.setSellingPrice(10);
        item.setGoodsCount(goodsCount);
        return item;
    }

    static IndexConfig indexConfig(Long configId, Long goodsId) {
        IndexConfig indexConfig = new IndexConfig();
        indexConfig.setConfigId(configId);
        indexConfig.setConfigName("config");
        indexConfig.setConfigType((byte) IndexConfigTypeEnum.INDEX_GOODS_HOT.getType());
        indexConfig.setGoodsId(goodsId);
        indexConfig.setRedirectUrl("/goods/" + goodsId);
        indexConfig.setConfigRank(1);
        return indexConfig;
    }

    static Carousel carousel(Integer carouselId) {
        Carousel carousel = new Carousel();
        carousel.setCarouselId(carouselId);
        carousel.setCarouselUrl("/carousel.png");
        carousel.setRedirectUrl("/redirect");
        carousel.setCarouselRank(1);
        return carousel;
    }

    static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
