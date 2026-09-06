package cn.net.zhu.seckill.business.service;


import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import java.util.List;

/**
 * 秒杀订单服务接口
 * 负责秒杀订单查询、列表分页统计、根据订单号查询、取消订单核心能力；
 * 订单来源于秒杀MQ消费者createOrder生成；cancelOrder用于超时未支付关闭订单、手动取消订单。
 *
 * @author 一只朱
 * @date 2026-09-06 10:55
 *
 * "Run the code. Run the world."
 */

public interface SeckillOrderTradeService {

    /**
     * 根据订单主键ID查询订单详情
     * @param id 订单数据库主键
     * @return SeckillOrderTradeEntity 订单实体，查不到返回null
     */
    SeckillOrderTradeEntity getOrderDetail(Long id);

    /**
     * 条件查询订单列表，用于分页展示
     * @param condition 查询条件实体：用户id、商品id、订单状态、支付状态等过滤条件
     * @return List<SeckillOrderTradeEntity> 订单结果集合
     */
    List<SeckillOrderTradeEntity> getOrderList(SeckillOrderTradeConditionEntity condition);

    /**
     * 根据条件统计订单总条数，配合分页列表使用
     * @param condition 查询过滤条件
     * @return int 符合条件的订单总数
     */
    int getOrderCount(SeckillOrderTradeConditionEntity condition);

    /**
     * 根据业务订单号（外部编码）查询订单详情
     * @param orderNo 业务订单编码
     * @return SeckillOrderTradeEntity 订单实体，查不到返回null
     */
    SeckillOrderTradeEntity getOrderDetailByOrderNo(String orderNo);

    /**
     * 取消订单
     * 处理逻辑：更新订单状态为已取消，回滚商品锁定库存；
     * 主要供订单超时延时消息调用，也可用于用户主动取消未支付订单
     * @param orderNo 业务订单编号
     * @return boolean true取消成功；false取消失败（订单已支付/已完成不可取消）
     */
    boolean cancelOrder(String orderNo);
}

/*
====================业务总结====================
1、模块职责：秒杀订单领域服务接口；提供订单CRUD查询能力、订单取消与库存回滚能力；
2、调用链路：
前端订单页面 → Controller → SeckillOrderTradeService → SeckillOrderTradeMapper；
订单超时MQ消费者 → cancelOrder() 自动关闭过期未支付订单；
3、核心流程：
① getOrderDetail / getOrderDetailByOrderNo：主键/业务单号查询单条订单详情；
② getOrderList + getOrderCount：配套实现分页查询订单列表；
③ cancelOrder：取消未支付订单，更新订单状态，回滚数据库锁定库存。
4、技术设计亮点：
- 区分数据库主键id与业务orderNo；对外接口优先使用orderNo，避免暴露数据库自增ID；
- 分页拆分为查询列表+统计总数两个方法，适配前端分页组件；
- cancelOrder统一收拢订单取消逻辑，业务调用方无需关心库存回滚细节。
5、风险点 & 潜在坑：
- cancelOrder仅允许取消待支付订单；已支付订单不能取消；需要做状态校验；
- 取消订单回滚DB库存后，需要同步更新Redis库存以及本地低库存标记，否则缓存与DB不一致；
- orderNo需要建立数据库索引，保证根据订单号查询性能。
6、模块关联：对接SeckillOrderTradeMapper；被OrderTimeoutService超时消息调用；和秒杀下单模块联动。
7、生产注意事项：订单量大时，列表查询注意条件索引优化；避免全表扫描。
*/

