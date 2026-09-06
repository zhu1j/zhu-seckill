package cn.net.zhu.seckill.business.service.impl;

import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import cn.net.zhu.seckill.business.enums.OrderStatusEnum;
import cn.net.zhu.seckill.business.mapper.seckill.SeckillOrderTradeMapper;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.service.SeckillOrderTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 秒杀订单服务实现类
 * 实现秒杀订单查询、分页统计、订单取消能力；
 * 查询时自动填充订单状态文本描述；cancelOrder用于超时未支付关闭订单，仅待支付订单允许取消；
 * 注意：当前实现只更新数据库订单状态，缺少库存回滚、Redis缓存同步逻辑，需要上层业务补充。
 *
 * @author 一只朱
 * @date 2026-09-06 10:58
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderTradeServiceImpl implements SeckillOrderTradeService {

    /**
     * 秒杀订单Mapper，负责订单数据库CRUD操作
     */
    private final SeckillOrderTradeMapper seckillOrderTradeMapper;

    /**
     * 商品服务，操作商品、ES商品信息
     */
    private final ProductService productService;

    /**
     * 根据订单主键ID查询订单详情
     * @param id 订单数据库主键
     * @return SeckillOrderTradeEntity 订单实体；查询到后填充状态文字描述，查不到返回null
     */
    @Override
    public SeckillOrderTradeEntity getOrderDetail(Long id) {
        SeckillOrderTradeEntity order = seckillOrderTradeMapper.findById(id);
        if (order != null) {
            // 根据状态码枚举翻译状态描述，供前端页面展示
            order.setOrderStatusString(OrderStatusEnum.getDescByValue(order.getOrderStatus()));
        }
        return order;
    }

    /**
     * 条件查询订单列表，用于分页
     * @param condition 查询过滤条件
     * @return List<SeckillOrderTradeEntity> 订单集合，每条订单填充状态文字描述
     */
    @Override
    public List<SeckillOrderTradeEntity> getOrderList(SeckillOrderTradeConditionEntity condition) {
        List<SeckillOrderTradeEntity> list = seckillOrderTradeMapper.searchByCondition(condition);
        if (list != null) {
            for (SeckillOrderTradeEntity order : list) {
                // 批量翻译订单状态描述
                order.setOrderStatusString(OrderStatusEnum.getDescByValue(order.getOrderStatus()));
            }
        }
        return list;
    }

    /**
     * 根据条件统计订单总记录数，配合分页列表使用
     * @param condition 查询过滤条件
     * @return int 符合条件订单总数
     */
    @Override
    public int getOrderCount(SeckillOrderTradeConditionEntity condition) {
        return seckillOrderTradeMapper.searchCount(condition);
    }

    /**
     * 根据业务订单号查询订单详情
     * @param orderNo 业务订单编码
     * @return SeckillOrderTradeEntity 订单实体，查不到返回null
     */
    @Override
    public SeckillOrderTradeEntity getOrderDetailByOrderNo(String orderNo) {
        return seckillOrderTradeMapper.findByCode(orderNo);
    }

    /**
     * 取消订单
     * 仅待支付状态(1)订单允许取消；更新订单状态为已取消(4)；
     * 开启事务，任意异常回滚；
     * 注意：本方法只修改订单状态，**没有执行库存回滚**，库存回滚逻辑需要在调用方完成。
     * @param orderNo 业务订单编号
     * @return boolean true更新成功；false：订单不存在、状态不允许取消、发生异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        log.info("开始取消订单，订单号：{}", orderNo);
        try {
            SeckillOrderTradeEntity order = seckillOrderTradeMapper.findByCode(orderNo);
            if (order == null) {
                log.error("订单不存在，订单号：{}", orderNo);
                return false;
            }
            // 业务规则：只有待支付订单才允许取消
            if (order.getOrderStatus() != 1) {
                log.error("订单状态不允许取消，当前状态：{}", order.getOrderStatus());
                return false;
            }
            // 修改订单状态为已取消
            order.setOrderStatus(4);
            order.setUpdateTime(new Date());
            int result = seckillOrderTradeMapper.update(order);
            log.info("订单取消成功，订单号：{}", orderNo);
            return result > 0;
        } catch (Exception e) {
            log.error("取消订单异常", e);
            return false;
        }
    }
}

/*
====================业务总结====================
1、模块职责：秒杀订单服务实现；提供订单详情、分页列表、统计、按订单号查询、取消订单；查询时自动把数字状态码翻译成文本描述供前端渲染。
2、调用链路：
前端订单页面 → Controller → SeckillOrderTradeServiceImpl → SeckillOrderTradeMapper；
订单超时消费者 → cancelOrder() 关闭超时未支付订单。
3、核心流程：
① getOrderDetail / getOrderList：数据库查询订单，内存填充orderStatusString状态描述字段；
② getOrderCount：统计总数，支撑分页组件；
③ getOrderDetailByOrderNo：通过业务单号查询，不暴露数据库主键；
④ cancelOrder：事务保护；校验订单存在、校验订单状态；更新订单为已取消；异常捕获返回false。
4、技术设计亮点：
- 枚举翻译：数据库存储数字状态码，内存填充文本描述，数据库只存数值，减少存储；
- @Transactional 保证订单状态更新原子性；异常捕获打印日志，对外返回布尔结果；
- 业务层控制取消权限：只有待支付订单允许取消。
5、风险点 & 潜在坑：
- cancelOrder**只更新订单状态，没有回滚DB锁定库存、没有恢复Redis库存、没有清理本地低库存标记**；调用cancelOrder之后调用方必须补充库存回滚逻辑，否则库存数据不一致。
- orderStatusString只在内存设置，不会写回数据库；
- 异常全部catch吃掉，仅打日志，上层无法感知异常类型；
- 没有幂等处理，重复调用cancelOrder不会报错，直接返回false。
6、模块关联：依赖SeckillOrderTradeMapper；ProductService注入当前类但代码未使用；被OrderTimeoutService调用处理超时订单。
7、生产注意事项：
- 调用cancelOrder之后，必须同步执行：数据库锁定库存回滚、Redis库存incr恢复、清理本地LOCAL_LOW_STOCK标记；
- orderNo数据库务必建立索引，防止查询慢SQL；
- 大订单列表场景需要做好分页，禁止一次性全量加载订单数据。
*/
