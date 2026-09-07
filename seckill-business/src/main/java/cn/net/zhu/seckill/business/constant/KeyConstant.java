package cn.net.zhu.seckill.business.constant;

/**
 *  Redis 键常量类（Redis Key 前缀）
 *
 * @author 一只朱
 * @date 2026-08-22 19:39
 *
 * "Run the code. Run the world."
 */

public class KeyConstant {
    private KeyConstant() {

    }

    /**
     * 秒杀商品详情Redis中key的前缀
     */
    public static final String SECKILL_PRODUCT_DETAIL_PFREFIX = "seckillProductDetail:";

    /**
     * 秒杀商品库存
     */
    public static final String SECKILL_PRODUCT_STOCK_PREFIX = "seckillProductStock:";
}