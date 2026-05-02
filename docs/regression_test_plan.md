# newbee-mall 回归测试计划

## 一、结论

本次单元测试中两个未通过用例均为合理测试，属于测试规划暴露出的生产代码行为偏差；测试代码无需调整。

后续修复生产代码后，应重新运行对应测试类或完整 `mvn test` 进行回归验证。

## 二、失败用例与修复建议

### 1. 用户修改资料时 session 用户不存在

- 归属规划：`docs/unit_test_plan.md` 中 “用户注册与登录” 健壮性测试第 4 条：修改用户信息时，session 中用户不存在或数据库中用户不存在，返回 `null`。
- 对应测试：`NewBeeMallUserServiceImplTest.updateUserInfo_whenSessionUserMissing_returnsNull`
- 当前错误：
  - `NewBeeMallUserServiceImpl.updateUserInfo()` 直接从 session 读取 `NewBeeMallUserVO` 后调用 `userTemp.getUserId()`。
  - 当 session 中没有 `Constants.MALL_USER_SESSION_KEY` 时，`userTemp` 为 `null`，触发 `NullPointerException`。
- 当前生产代码位置：
  - `src/main/java/ltd/newbee/mall/service/impl/NewBeeMallUserServiceImpl.java:78-79`
- 建议修复：
  - 在读取 session 用户后先判断 `userTemp == null`，为空时直接返回 `null`。
  - 可同时防御 `httpSession == null`，避免调用 `httpSession.getAttribute(...)` 时出现空指针。
- 回归验证：
  - 运行 `NewBeeMallUserServiceImplTest`。
  - 确认 `updateUserInfo_whenSessionUserMissing_returnsNull` 通过。
  - 确认 `updateUserInfo_whenDatabaseUserMissing_returnsNull` 和正常更新资料用例仍通过。

### 2. 保存订单项失败时异常类型不匹配

- 归属规划：`docs/unit_test_plan.md` 中 “订单创建与支付” 健壮性测试第 6 条：删除购物项失败、扣减库存失败、保存订单失败或保存订单项失败时，抛出对应业务异常。
- 对应测试：`NewBeeMallOrderServiceImplTest.saveOrder_whenOrderItemsSaveFails_throwsOrderGenerateError`
- 当前错误：
  - `newBeeMallOrderItemMapper.insertBatch(newBeeMallOrderItems)` 返回失败时，当前代码抛出 `ServiceResultEnum.ORDER_PRICE_ERROR`，错误信息为 `订单价格异常！`。
  - 该分支实际含义是订单项快照保存失败，不是订单总价小于 1。
  - `ServiceResultEnum` 已存在更匹配的 `ORDER_GENERATE_ERROR("生成订单异常！")`。
- 当前生产代码位置：
  - `src/main/java/ltd/newbee/mall/service/impl/NewBeeMallOrderServiceImpl.java:251-255`
  - `src/main/java/ltd/newbee/mall/common/ServiceResultEnum.java:60-62`
- 建议修复：
  - 将订单项保存失败分支从 `ServiceResultEnum.ORDER_PRICE_ERROR.getResult()` 改为 `ServiceResultEnum.ORDER_GENERATE_ERROR.getResult()`。
  - 保留订单总价校验失败分支继续使用 `ORDER_PRICE_ERROR`。
- 回归验证：
  - 运行 `NewBeeMallOrderServiceImplTest`。
  - 确认 `saveOrder_whenOrderItemsSaveFails_throwsOrderGenerateError` 通过。
  - 确认 `saveOrder_whenTotalPriceIsZero_throwsOrderPriceError` 仍然抛出 `订单价格异常！`。

## 三、建议回归范围

修复以上两个生产代码问题后，建议至少运行：

```bash
mvn test -Dtest=NewBeeMallUserServiceImplTest,NewBeeMallOrderServiceImplTest
```

最终合并前运行完整测试：

```bash
mvn test
```
