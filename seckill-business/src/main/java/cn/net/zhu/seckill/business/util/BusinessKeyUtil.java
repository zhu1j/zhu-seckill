package cn.net.zhu.seckill.business.util;

/**
 *  业务Key工具类
 *
 * @author 一只朱
 * @date 2026-08-27 22:20
 *
 * "Run the code. Run the world."
 */

/** 抽象常量工具类，专门统一生成 Redis 的 key 字符串
 * - `abstract class`：抽象类，防止被 new 实例化；再加私有构造函数 `private BusinessKeyUtil(){}`，双重保护，只能静态调用。
 * - 全部静态方法：直接 `BusinessKeyUtil.getProductStockKey(id)` 使用。
 * - 作用：把项目所有 Redis key 全部收拢在一处管理，**禁止业务代码硬写字符串 key**。
 * ✅好处：
 * 1. key 统一命名规范，修改 key 前缀只改这一处；
 * 2. 避免手写字符串拼写错误；
 * 3. 方便阅读，一眼看懂每个 redis key 业务含义
 */

public abstract class BusinessKeyUtil {
    private BusinessKeyUtil(){}

    /**
     * 业务含义：标记某个用户对某个商品是否已经参与秒杀（防重复下单，一人一单限制）
     * - key：`用户+商品id` 组合
     * - 场景：限制同一个用户不能重复秒杀同一个商品。(比如用户已经成功抢到商品 1001，这个 key 就打上标记，再来请求直接拒绝)
     * @param seckillProductId
     * @param userName
     * @return
     */
    public static String getUserSeckillProductKey(Long seckillProductId,String userName){
        return String.format("userSeckillProduct:%d_%s",seckillProductId,userName);
    }

    /**
     * 秒杀商品库存 Redis Key（最核心）
     * - 存的是该商品剩余库存数值，配合之前写的 `RedisUtil.decrement()` 原子扣库存。
     * > 秒杀预热阶段，把数据库库存加载到这个 redis key。
     * @param seckillProductId
     * @return
     */
    public static String getProductStockKey(Long seckillProductId) {
        return String.format("seckillProductStock:%d", seckillProductId);
    }

    /**
     *  商品秒杀结束标记 key
     * - 标记该商品是否卖完 / 秒杀结束。
     * - 业务：当库存扣减到 0，设置这个 key，后续请求直接快速失败，不再往下走逻辑（缓存熔断，减少 redis、db 压力）。
     * > 存在 = 秒杀结束；不存在 = 还可以抢。
     * @param seckillProductId
     * @return
     */
    public static String getProductOverKey(Long seckillProductId){
        return String.format("seckillProductOver:%d",seckillProductId);
    }

    /**
     * 用户全局秒杀总次数
     * 按用户统计，限制用户总共能抢多少次（不是针对单个商品，是用户维度总次数限制）。
     * 比如：一个账号最多允许成功秒杀 3 次，用这个 key 做 increment 计数。
     * @param userName
     * @return
     */
    public static String getUserSeckillCountKey(String userName){
        return String.format("userSeckillCount:%s",userName);
        /**
         * > 和 `getUserSeckillProductKey`区分：
         *
         * - `userSeckillProduct:1001_zhangsan`：**单个商品一人一单**
         * - `userSeckillCount:zhangsan`：**该用户所有秒杀一共最多 N 次**
         */
    }

    /**
     * > 用户秒杀流程状态 key，异步秒杀常用。
     * > 场景：收到秒杀请求，消息队列异步处理下单，前端轮询查询秒杀结果。
     * > key 里面存状态：
     * - `0`：处理中
     * - `1`：秒杀成功
     * - `-1`：秒杀失败
     * (用户提交秒杀后，立刻返回 “排队中”，前端拿着商品 id + 用户名查询这个 key，获取最终秒杀结果。)
     * @param seckillProductId
     * @param userName
     * @return
     */
    public static String getSeckillProcessStatusKey(Long seckillProductId,String userName){
        return String.format("seckillProcessStatus:%d_%s",seckillProductId,userName);
    }
}
