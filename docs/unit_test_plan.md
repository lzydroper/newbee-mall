# newbee-mall 单元测试方案

## 一、测试范围

本方案仅规划当前系统最主要业务逻辑的单元测试，重点测试 `ltd.newbee.mall.service.impl` 包中的核心业务类。

测试方式：

- 使用 JUnit5 编写测试用例。
- 使用 Mockito 模拟 mapper 层，不连接 MySQL。
- 不启动完整 Spring Boot 上下文。
- 主要验证业务方法的返回值、异常分支、状态变化和关键 mapper 调用。

暂不测试：

- 工具类、公共枚举、拦截器。
- DAO 与 MyBatis XML 的真实数据库访问。
- Thymeleaf 页面渲染。
- 真实文件上传。
- 完整接口集成测试。

## 二、测试用例规划

### 1. 用户注册与登录

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallUserServiceImpl`
- 对应测试类：`NewBeeMallUserServiceImplTest`

a. 正确性测试：

1. 输入合法登录名和密码，且登录名未被注册，注册成功，返回 `success`。
2. 输入正确登录名和密码，且用户未被锁定，登录成功，返回 `success`。
3. 用户登录成功后，用户信息被写入 session。
4. 用户昵称长度超过 7 位时，登录后昵称被截断为前 7 位加 `..`。
5. 用户修改昵称、地址、个性签名时，数据库更新成功后返回新的用户信息。

b. 健壮性测试：

1. 输入的登录名已存在，注册失败，返回“用户名已存在！”。
2. 输入错误的登录名或密码，登录失败，返回“登录失败！”。
3. 用户账号被锁定时登录，返回“用户已被禁止登录！”。
4. 修改用户信息时，session 中用户不存在或数据库中用户不存在，返回 `null`。
5. 批量锁定用户时，输入的用户 ID 数组为空，返回 `false`。

### 2. 管理员登录与资料维护

测试包及类：

- `ltd.newbee.mall.service.impl.AdminUserServiceImpl`
- 对应测试类：`AdminUserServiceImplTest`

a. 正确性测试：

1. 输入正确管理员账号和密码，密码经 MD5 加密后查询成功，返回管理员信息。
2. 输入正确原密码和新密码，修改密码成功，返回 `true`。
3. 输入新的登录名和昵称，修改管理员资料成功，返回 `true`。

b. 健壮性测试：

1. 输入错误账号或密码，登录失败，返回 `null`。
2. 修改密码时原密码错误，返回 `false`。
3. 修改密码时管理员不存在，返回 `false`。
4. 修改名称时管理员不存在，返回 `false`。
5. 数据库更新失败时，修改密码或修改名称返回 `false`。

### 3. 商品管理

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallGoodsServiceImpl`
- 对应测试类：`NewBeeMallGoodsServiceImplTest`

a. 正确性测试：

1. 输入合法商品信息，商品所属分类存在且为三级分类，商品名不重复，创建商品成功，返回 `success`。
2. 输入合法商品 ID 和商品信息，商品存在且商品名不重复，更新商品成功，返回 `success`。
3. 输入商品 ID 数组和合法上下架状态，批量修改上下架状态成功，返回 `true`。
4. 输入合法搜索条件，返回商品分页结果。
5. 查询存在的商品 ID，返回商品详情。

b. 健壮性测试：

1. 商品所属分类不存在，创建或更新失败，返回“分类数据异常！”。
2. 商品所属分类不是三级分类，创建或更新失败，返回“分类数据异常！”。
3. 同一分类下商品名已存在，创建失败，返回“已存在相同的商品信息！”。
4. 更新商品时，商品 ID 不存在，返回“未查询到记录！”。
5. 更新商品时，新的商品名与同分类下其他商品重复，返回“已存在相同的商品信息！”。
6. 查询不存在的商品 ID，抛出“商品不存在！”异常。
7. 商品搜索结果中商品名称或简介过长时，应按当前业务规则截断。

### 4. 商品分类管理

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallCategoryServiceImpl`
- 对应测试类：`NewBeeMallCategoryServiceImplTest`

a. 正确性测试：

1. 输入合法分类名称、分类级别和父分类 ID，且同级分类名不重复，创建分类成功，返回 `success`。
2. 输入存在的分类 ID 和合法分类信息，更新分类成功，返回 `success`。
3. 输入非空分类 ID 数组，批量删除分类成功，返回 `true`。
4. 首页存在一级、二级、三级分类数据时，成功组装首页三级分类结构。
5. 搜索页输入三级分类 ID 时，成功返回当前三级分类及其同级分类信息。

b. 健壮性测试：

1. 同级分类名已存在，创建分类失败，返回“已存在同级同名的分类！”。
2. 更新分类时分类 ID 不存在，返回“未查询到记录！”。
3. 更新分类时新分类名与同级其他分类重复，返回“已存在同级同名的分类！”。
4. 删除分类时输入的 ID 数组为空，返回 `false`。
5. 首页一级分类为空时，返回 `null`。
6. 搜索页输入的分类 ID 不存在，返回 `null`。
7. 搜索页输入的分类不是三级分类，返回 `null`。

### 5. 购物车管理

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallShoppingCartServiceImpl`
- 对应测试类：`NewBeeMallShoppingCartServiceImplTest`

a. 正确性测试：

1. 输入用户 ID、商品 ID 和购买数量，商品存在且购物车未超限，添加购物车成功，返回 `success`。
2. 添加购物车时，同一用户购物车中已存在该商品，则更新原购物项数量。
3. 输入存在的购物项 ID 和合法购买数量，更新购物项成功，返回 `success`。
4. 输入属于当前用户的购物项 ID，删除购物项成功，返回 `true`。
5. 查询当前用户购物车，返回购物车商品列表。
6. 商品名称超过 28 位时，购物车展示名称被截断为前 28 位加 `...`。

b. 健壮性测试：

1. 添加购物车时商品不存在，返回“商品不存在！”。
2. 添加或更新购物车时，单个商品购买数量超过 `5`，返回“超出单个商品的最大购买数量！”。
3. 添加购物车时，购物车商品总数超过 `13`，返回“超出购物车最大容量！”。
4. 更新购物车时购物项不存在，返回“未查询到记录！”。
5. 更新购物车时购物项不属于当前用户，返回“无权限！”。
6. 删除购物项时购物项不存在，返回 `false`。
7. 删除购物项时购物项不属于当前用户，返回 `false`。

### 6. 订单创建与支付

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallOrderServiceImpl`
- 对应测试类：`NewBeeMallOrderServiceImplTest`

a. 正确性测试：

1. 用户购物车中商品均存在、均已上架、库存充足，创建订单成功，返回订单号。
2. 创建订单成功后，删除对应购物项。
3. 创建订单成功后，扣减商品库存。
4. 创建订单成功后，保存订单主表和订单项快照。
5. 输入待支付订单号和支付类型，支付成功，订单状态更新为已支付，支付状态更新为支付成功。

b. 健壮性测试：

1. 创建订单时购物车商品为空，返回或抛出“购物车数据异常！”。
2. 创建订单时商品不存在，抛出“购物车数据异常！”。
3. 创建订单时商品已下架，抛出“已下架，无法生成订单”相关异常。
4. 创建订单时购买数量大于库存，抛出“库存不足！”。
5. 创建订单时订单总价小于 `1`，抛出“订单价格异常！”。
6. 删除购物项失败、扣减库存失败、保存订单失败或保存订单项失败时，抛出对应业务异常。
7. 支付时订单不存在，返回“订单不存在！”。
8. 支付时订单不是待支付状态，返回“订单状态异常！”。

### 7. 订单状态管理

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallOrderServiceImpl`
- 对应测试类：`NewBeeMallOrderServiceImplTest`

a. 正确性测试：

1. 后台对已支付订单执行配货完成操作，订单状态更新成功，返回 `success`。
2. 后台对已支付或配货完成订单执行出库操作，订单状态更新成功，返回 `success`。
3. 后台关闭允许关闭的订单，订单关闭成功，并恢复商品库存，返回 `success`。
4. 前台用户取消自己的可取消订单，订单关闭成功，并恢复商品库存，返回 `success`。
5. 前台用户确认收货时，订单状态为出库成功，更新为交易成功，返回 `success`。
6. 查询用户自己的订单详情时，订单和订单项存在，返回订单详情 VO。

b. 健壮性测试：

1. 后台配货时订单不是已支付状态，返回订单状态错误提示。
2. 后台出库时订单不是已支付或配货完成状态，返回订单状态错误提示。
3. 后台关闭订单时订单已删除、已关闭或已完成，返回订单不能关闭提示。
4. 取消订单时订单不存在，返回“订单不存在！”。
5. 取消订单时订单不属于当前用户，抛出“无权限！”异常。
6. 确认收货时订单不是出库成功状态，返回“订单状态异常！”。
7. 查询订单详情时订单不存在，抛出“订单不存在！”异常。
8. 查询订单详情时订单不属于当前用户，抛出“无权限！”异常。
9. 查询订单详情时订单项为空，抛出“订单项不存在！”异常。
10. 恢复库存失败时，抛出“关闭订单失败！”异常。

### 8. 首页配置与轮播图管理

测试包及类：

- `ltd.newbee.mall.service.impl.NewBeeMallIndexConfigServiceImpl`
- `ltd.newbee.mall.service.impl.NewBeeMallCarouselServiceImpl`
- 对应测试类：`NewBeeMallIndexConfigServiceImplTest`、`NewBeeMallCarouselServiceImplTest`

a. 正确性测试：

1. 输入合法首页配置类型、商品 ID 和排序值，且商品存在、配置不重复，新增首页配置成功，返回 `success`。
2. 输入存在的首页配置 ID 和合法配置内容，更新首页配置成功，返回 `success`。
3. 输入非空首页配置 ID 数组，删除首页配置成功，返回 `true`。
4. 查询首页配置商品时，成功返回首页商品 VO 列表。
5. 输入合法轮播图地址、跳转地址和排序值，新增轮播图成功，返回 `success`。
6. 输入存在的轮播图 ID 和合法轮播图信息，更新轮播图成功，返回 `success`。
7. 查询首页轮播图时，成功返回首页轮播图 VO 列表。

b. 健壮性测试：

1. 新增或更新首页配置时商品不存在，返回“商品不存在！”。
2. 新增首页配置时同类型下商品配置已存在，返回“已存在相同的首页配置项！”。
3. 更新首页配置时配置 ID 不存在，返回“未查询到记录！”。
4. 更新首页配置时新配置与其他配置重复，返回“已存在相同的首页配置项！”。
5. 删除首页配置时 ID 数组为空，返回 `false`。
6. 更新轮播图时轮播图 ID 不存在，返回“未查询到记录！”。
7. 删除轮播图时 ID 数组为空，返回 `false`。
8. 数据库插入、更新或删除返回失败时，返回 `database error` 或 `false`。

## 三、边界测试设计

边界测试包：

- `ltd.newbee.mall.service.boundary`
- 公共测试数据构造类：`BoundaryTestFixtures`

边界测试同样使用 JUnit5 与 Mockito，不连接真实数据库，不启动完整 Spring Boot 上下文。测试类按业务服务拆分，重点验证临界值两侧的返回值、异常信息、状态变化和关键对象字段。

### 1. 用户登录展示边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallUserServiceImpl`

对应测试类：

- `BoundaryNewBeeMallUserTest`

测试用例：

1. `login_withNicknameLengthSeven_doesNotTruncateNickname`：用户昵称长度为 `7` 时，登录成功后 session 中昵称不截断。
2. `login_withNicknameLengthEight_truncatesNickname`：用户昵称长度为 `8` 时，登录成功后 session 中昵称截断为前 `7` 位加 `..`。

### 2. 购物车数量与展示边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallShoppingCartServiceImpl`

对应测试类：

- `BoundaryNewBeeMallShoppingCartTest`

测试用例：

1. `saveNewBeeMallCartItem_withGoodsCountEqualLimit_returnsSuccess`：单个商品购买数量等于 `Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER` 时，添加购物车成功。
2. `saveNewBeeMallCartItem_withGoodsCountOneAboveLimit_returnsLimitMessage`：单个商品购买数量超过上限 `1` 个时，返回单商品购买数量超限提示。
3. `saveNewBeeMallCartItem_whenUserHasTwelveItems_allowsOneMoreItem`：用户购物车已有 `Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER - 1` 个商品时，允许再新增一个商品。
4. `saveNewBeeMallCartItem_whenUserHasThirteenItems_rejectsOneMoreItem`：用户购物车已有 `Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER` 个商品时，拒绝继续新增。
5. `getMyShoppingCartItems_withGoodsNameLengthTwentyEight_doesNotTruncateName`：购物车商品名长度为 `28` 时不截断。
6. `getMyShoppingCartItems_withGoodsNameLengthTwentyNine_truncatesName`：购物车商品名长度为 `29` 时截断为前 `28` 位加 `...`。

### 3. 订单创建、状态与金额边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallOrderServiceImpl`
- `ltd.newbee.mall.common.NewBeeMallOrderStatusEnum`

对应测试类：

- `BoundaryNewBeeMallOrderTest`

测试用例：

1. `saveOrder_whenGoodsIsUp_allowsOrder`：商品状态为上架时允许创建订单。
2. `saveOrder_whenGoodsIsDown_rejectsOrder`：商品状态为下架时拒绝创建订单，并抛出商品已下架相关异常。
3. `saveOrder_whenStockEqualsPurchaseCount_allowsOrder`：商品库存等于购买数量时允许创建订单。
4. `saveOrder_whenStockIsLessThanPurchaseCount_throwsStockError`：商品库存小于购买数量时抛出库存不足异常。
5. `saveOrder_whenTotalPriceIsOne_allowsOrder`：订单总价为 `1` 时允许创建订单，并写入订单总价。
6. `saveOrder_whenTotalPriceIsZero_throwsOrderPriceError`：订单总价为 `0` 时抛出订单价格异常。
7. `saveOrder_whenCartItemsEmpty_throwsShoppingCartDataError`：购物车列表为空时抛出购物车数据异常。
8. `getOrderDetailByOrderNo_mapsEveryOrderStatusToDisplayName`：除 `DEFAULT` 外的所有订单状态枚举均可映射为对应展示名称。
9. `getOrderDetailByOrderNo_whenOrderItemsEmpty_throwsOrderItemNotExist`：订单项为空时不返回正常订单详情，抛出订单项不存在异常。
10. `paySuccess_whenOrderIsPrePay_updatesStatusToPaid`：订单处于待支付状态时，支付成功后状态更新为已支付。
11. `finishOrder_whenOrderIsExpress_updatesStatusToSuccess`：订单处于出库成功状态时，确认收货后状态更新为交易成功。
12. `getNewBeeMallOrderStatusEnumByStatus_returnsDefinedClosedStatuses`：手动关闭、超时关闭、商家关闭状态码均能映射为对应订单状态枚举。

### 4. 首页配置商品展示边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallIndexConfigServiceImpl`

对应测试类：

- `BoundaryNewBeeMallIndexConfigTest`

测试用例：

1. `getConfigGoodsesForIndex_withNameLengthThirty_doesNotTruncateName`：首页商品名长度为 `30` 时不截断。
2. `getConfigGoodsesForIndex_withNameLengthThirtyOne_truncatesName`：首页商品名长度为 `31` 时截断为前 `30` 位加 `...`。
3. `getConfigGoodsesForIndex_withIntroLengthTwentyTwo_doesNotTruncateIntro`：首页商品简介长度为 `22` 时不截断。
4. `getConfigGoodsesForIndex_withIntroLengthTwentyThree_truncatesIntro`：首页商品简介长度为 `23` 时截断为前 `22` 位加 `...`。

### 5. 商品展示状态边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallGoodsServiceImpl`

对应测试类：

- `BoundaryNewBeeMallGoodsTest`

测试用例：

1. `getNewBeeMallGoodsById_whenGoodsIsUp_returnsGoodsForDisplay`：商品状态为上架时，查询商品详情返回原商品对象，并保留上架状态。

### 6. 批量操作空集合边界

测试生产类：

- `ltd.newbee.mall.service.impl.NewBeeMallUserServiceImpl`
- `ltd.newbee.mall.service.impl.NewBeeMallCategoryServiceImpl`
- `ltd.newbee.mall.service.impl.NewBeeMallGoodsServiceImpl`
- `ltd.newbee.mall.service.impl.NewBeeMallIndexConfigServiceImpl`
- `ltd.newbee.mall.service.impl.NewBeeMallCarouselServiceImpl`

对应测试类：

- `BatchOperationBoundaryTest`

测试用例：

1. `lockUsers_withEmptyIds_returnsFalse`：批量锁定用户时，ID 数组为空返回 `false`。
2. `deleteCategories_withEmptyIds_returnsFalse`：批量删除分类时，ID 数组为空返回 `false`。
3. `batchUpdateSellStatus_withEmptyIds_returnsFalse`：批量更新商品上下架状态时，ID 数组为空返回 `false`。
4. `deleteIndexConfigs_withEmptyIds_returnsFalse`：批量删除首页配置时，ID 数组为空返回 `false`。
5. `deleteCarousels_withEmptyIds_returnsFalse`：批量删除轮播图时，ID 数组为空返回 `false`。

## 四、执行建议

1. 在 `src/test/java/ltd/newbee/mall/service/impl` 下创建对应测试类。
2. 优先实现订单、购物车、商品、用户相关测试。
3. 每个测试方法只验证一个明确业务场景。
4. 测试用例不访问真实数据库，所有 mapper 均使用 Mockito 模拟。
5. 执行命令：

```bash
mvn test
```

本阶段仅规划单元测试方案，不新增具体测试代码。
