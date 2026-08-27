package cn.net.zhu.seckill.business.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 订单号生成工具类
 * 生成秒杀订单编号，固定长度24位
 * 规则：XS + 时间戳(yyyyMMddHHmmss 14位) + 6位随机数，多余部分截断补齐至24位
 *
 * @author 一只朱
 * @date 2026-08-27 22:36
 *
 * "Run the code. Run the world."
 */
public class OrderCodeUtil {
    /**
     * 时间格式化：年月日时分秒 yyyyMMddHHmmss，输出14位数字字符串
     * 注意：SimpleDateFormat非线程安全，静态常量多线程环境存在风险
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * 随机数对象，用于生成6位随机数字
     */
    private static final Random RANDOM = new Random();

    /**
     * 生成24位秒杀订单号
     * 格式示例：XS20260827223612123456000000
     * 组成：XS(2) + yyyyMMddHHmmss(14) + 6位随机数，拼接后截取前24位
     * @return 24位订单编号字符串
     */
    public static String generateOrderCode() {
        // 前缀 XS + 当前时间：XS + yyyyMMddHHmmss，共16位
        String prefix = "XS" + DATE_FORMAT.format(new Date());
        // 生成 [100000 ~ 999999] 范围的6位随机整数
        int randomNum = RANDOM.nextInt(900000) + 100000;
        // 拼接字符串后截取前24位；后面拼接000000是为字符串长度不足24时做填充
        return (prefix + randomNum + "000000").substring(0, 24);
    }
}
