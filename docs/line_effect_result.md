# JProfiler 行代码运行效能测试结果

## 测试截图结论

本次对比采用两组均为 11 次 `/index` 调用的 JProfiler 结果。两组结果中仍然包含明显的 JProfiler agent 和 JVM 启动相关开销，因此不应把 `Agent.startAgent` 计入业务代码性能评价。

优化前截图中的主要数据：

- `jdk.internal.agent.Agent.startAgent`：52.0%，3844 ms，属于 JProfiler agent 启动与 JVM 启动相关开销。
- `org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run`：48.0%，3548 ms，代表 Tomcat 请求线程中的运行时间。
- `HTTP: /index`：18.0%，1334 ms，11 次调用，平均约 121.3 ms/次。
- `IndexController.indexPage`：8.5%，630 ms，11 次调用，平均约 57.3 ms/次。
- `NewBeeMallCategoryServiceImpl.getCategoriesForIndex`：6.9%，509 ms，11 次调用，平均约 46.3 ms/次。

优化后截图中的主要数据：

- `HTTP: /index`：18.6%，1316 ms，11 次调用，平均约 119.6 ms/次。
- `IndexController.indexPage`：9.2%，649 ms，11 次调用，平均约 59.0 ms/次。
- `NewBeeMallCategoryServiceImpl.getCategoriesForIndex`：7.3%，516 ms，11 次调用，平均约 46.9 ms/次。

从 11 次调用的平均值看，优化前后首页整体耗时基本持平，`/index` 平均耗时仅从约 121.3 ms/次下降到约 119.6 ms/次，变化很小。`getCategoriesForIndex()` 平均耗时从约 46.3 ms/次变为约 46.9 ms/次，也没有体现出明显下降。

因此，本次 JProfiler 对比可以说明：当前小数据量场景下，索引优化对首页接口的可观测耗时改善不明显；首页业务热点仍集中在分类树加载和 MyBatis/JDBC 查询链路，而不是普通 Java 行代码计算。

## 主要热点行

截图中最高的业务子调用是：

```java
List<GoodsCategory> firstLevelCategories = goodsCategoryMapper.selectByLevelAndParentIdsAndNumber(Collections.singletonList(0L), NewBeeMallCategoryLevelEnum.LEVEL_ONE.getLevel(), Constants.INDEX_CATEGORY_NUMBER);
```

位置：`src/main/java/ltd/newbee/mall/service/impl/NewBeeMallCategoryServiceImpl.java`，`getCategoriesForIndex()` 中获取一级分类的查询。

优化前 JProfiler 显示该 mapper 调用：

- `GoodsCategoryMapper.selectByLevelAndParentIdsAndNumber`，line 95：361 ms，11 次调用，平均约 32.8 ms/次。
- 同一 mapper 的二级分类查询，line 103：48.507 ms，11 次调用，平均约 4.4 ms/次。
- 同一 mapper 的三级分类查询，line 99：41.131 ms，11 次调用，平均约 3.7 ms/次。

优化后 JProfiler 显示该 mapper 调用：

- `GoodsCategoryMapper.selectByLevelAndParentIdsAndNumber`，line 95：395 ms，11 次调用，平均约 35.9 ms/次。
- 同一 mapper 的二级分类查询，line 103：41.780 ms，11 次调用，平均约 3.8 ms/次。
- 同一 mapper 的三级分类查询，line 99：29.229 ms，11 次调用，平均约 2.7 ms/次。

三次分类 mapper 查询合计对比：

- 优化前：约 450.638 ms / 11 次，平均约 41.0 ms/次。
- 优化后：约 466.009 ms / 11 次，平均约 42.4 ms/次。

结论：这行代码仍然是当前截图里的主要业务热点，但优化前后在 JProfiler 采样结果中没有出现明显下降。更准确地说，问题不是 Java 这一行赋值或 `List` 创建本身，而是该行触发了 MyBatis/JDBC 查询和结果映射；在当前小数据量下，这部分耗时受采样误差、连接状态、数据库缓存和 MyBatis 代理开销影响较大。

## 是否是表查询导致

判断为：主要是表查询导致的可能性较高。

该 mapper 对应 SQL 位于 `src/main/resources/mapper/GoodsCategoryMapper.xml`：

```sql
select
  category_id, category_level, parent_id, category_name, category_rank, is_deleted,
  create_time, create_user, update_time, update_user
from tb_newbee_mall_goods_category
where parent_id in (...)
  and category_level = ?
  and is_deleted = 0
order by category_rank desc
limit ?
```

该 SQL 的过滤和排序条件主要依赖：

- `parent_id`
- `category_level`
- `is_deleted`
- `category_rank`

但在 `src/main/resources/newbee_mall_schema.sql` 中，`tb_newbee_mall_goods_category` 当前只有主键：

```sql
PRIMARY KEY (`category_id`) USING BTREE
```

没有针对首页分类查询建立组合索引。这样数据库执行上述 SQL 时，容易出现全表扫描、额外排序或扫描行数偏多的问题。即使测试数据量不大，JProfiler 中 mapper proxy 统计到的时间也会包含 MyBatis、JDBC、连接获取、SQL 执行、结果集映射等成本，所以表现为这一行代码耗时集中。

MySQL `EXPLAIN` 已显示该查询优化前的执行计划为：

```text
Table scan on tb_newbee_mall_goods_category
Filter: is_deleted = 0 and category_level = 1 and parent_id = 0
Sort: category_rank DESC
Limit: 10
```

因此，可以确认优化前该查询没有命中合适索引，执行方式是表扫描后过滤，再额外排序。从 SQL 执行路径看，缺少组合索引是明确的结构性问题。

但需要注意：当前表数据量较小，`EXPLAIN` 中扫描行数约 93 行。在这种小数据量和本地数据库场景下，即使执行计划从表扫描变成索引扫描，JProfiler 中的接口总耗时也可能变化不明显，因为 MyBatis/JDBC、连接获取、结果集映射、采样误差和数据库缓存都会占据一定比例。

## 建议优化

### 1. 为分类查询增加组合索引

建议优先增加如下索引：

```sql
ALTER TABLE tb_newbee_mall_goods_category
ADD INDEX idx_category_parent_level_deleted_rank
(parent_id, category_level, is_deleted, category_rank);
```

该索引覆盖首页分类查询里的父级过滤、分类级别过滤、删除标识过滤和排序字段，可减少扫描与排序成本。

如果希望更贴合 `category_level + parent_id` 的查询习惯，也可以评估：

```sql
ALTER TABLE tb_newbee_mall_goods_category
ADD INDEX idx_category_level_parent_deleted_rank
(category_level, parent_id, is_deleted, category_rank);
```

两种索引不建议盲目同时添加，应通过 `EXPLAIN` 和实际查询耗时选择一个更适合当前数据分布的索引。

### 2. 对首页分类树做缓存

首页分类树属于读多写少数据。每次访问首页都重新查三级分类并组装 VO，会造成重复数据库访问。

建议缓存：

- 首页分类树 `getCategoriesForIndex()`
- 首页轮播图
- 首页热销商品、新品、推荐商品

可选方案：

- 单机部署：Spring Cache + Caffeine
- 多实例部署：Spring Cache + Redis

缓存失效时机：

- 后台新增、修改、删除商品分类后清理分类树缓存。
- 后台调整轮播或首页配置商品后清理对应首页缓存。

### 3. 测试方法继续收敛

为了让行代码运行效能结果更准确，建议：

- 应用完全启动后再开始 JProfiler 录制。
- 先预热 `/index` 几次，再正式采样。
- 单独采集 `/index`，不要把 `Agent.startAgent` 和 `SpringApplication.run` 纳入业务结论。
- 开启 JProfiler JDBC Probe，确认具体 SQL 的执行耗时。
- 对 SQL 执行 `EXPLAIN`，确认是否使用索引、扫描行数和是否出现 filesort。

## 最终结论

本次 11 次调用对比显示，`getCategoriesForIndex()` 仍是首页请求中的主要业务热点，其中获取一级分类的 `selectByLevelAndParentIdsAndNumber(...)` 调用耗时最高。该行代码的问题本质上不是 Java `List` 操作慢，而是它触发了 MyBatis/JDBC 查询和结果映射。

MySQL `EXPLAIN` 已经证明优化前分类查询存在表扫描和额外排序，因此缺少适配 `parent_id + category_level + is_deleted + category_rank` 的组合索引是明确的数据库执行路径问题。增加组合索引是合理优化。

但从优化前后 JProfiler 的 11 次调用对比看，当前小数据量场景下接口平均耗时变化很小，不能说明索引优化带来了明显可观测收益。这更适合得出以下结论：

- 首页 `/index` 的主要耗时集中在数据库访问链路，而不是 Java 普通行代码计算。
- 当前项目首页模块没有表现出明显的 Java 行代码执行效率问题。
- 数据库索引属于结构性优化，能避免数据量增长后全表扫描和排序放大。
- Redis 缓存不是本次截图能证明的根因，只能作为减少首页重复数据库查询的进一步优化方案。
