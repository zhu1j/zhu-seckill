-- 修复支付表字段名称不匹配问题
-- 问题：Mapper期望的字段名与数据库表实际字段名不一致

-- 1. 添加缺失的字段 third_party_transaction_no
ALTER TABLE `seckill_payment` ADD COLUMN `third_party_transaction_no` varchar(64) DEFAULT NULL COMMENT '第三方交易流水号' AFTER `payment_status`;

-- 2. 添加缺失的字段 third_party_refund_no  
ALTER TABLE `seckill_payment` ADD COLUMN `third_party_refund_no` varchar(64) DEFAULT NULL COMMENT '第三方退款流水号' AFTER `refund_time`;

-- 3. 删除原有的字段 third_party_trade_no (如果存在)
-- ALTER TABLE `seckill_payment` DROP COLUMN IF EXISTS `third_party_trade_no`;

-- 验证表结构
-- DESCRIBE `seckill_payment`;