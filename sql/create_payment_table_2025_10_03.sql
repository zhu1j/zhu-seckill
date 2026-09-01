-- 支付表
CREATE TABLE `seckill_payment` (
  `id` bigint NOT NULL COMMENT '支付ID',
  `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_code` varchar(30) NOT NULL COMMENT '订单编码',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户名称',
  `payment_amount` decimal(10,3) NOT NULL COMMENT '支付金额',
  `payment_method` tinyint NOT NULL DEFAULT '1' COMMENT '支付方式 1:支付宝 2:微信 3:银行卡',
  `payment_status` tinyint NOT NULL DEFAULT '1' COMMENT '支付状态 1:待支付 2:支付中 3:支付成功 4:支付失败 5:已退款',
  `third_party_trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易流水号',
  `payment_time` datetime(3) DEFAULT NULL COMMENT '支付时间',
  `callback_time` datetime(3) DEFAULT NULL COMMENT '回调时间',
  `refund_amount` decimal(10,3) DEFAULT '0.000' COMMENT '退款金额',
  `refund_time` datetime(3) DEFAULT NULL COMMENT '退款时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_user_id` bigint NOT NULL COMMENT '创建人ID',
  `create_user_name` varchar(30) NOT NULL COMMENT '创建人名称',
  `create_time` datetime(3) NOT NULL COMMENT '创建时间',
  `update_user_id` bigint DEFAULT NULL COMMENT '修改人ID',
  `update_user_name` varchar(30) DEFAULT NULL COMMENT '修改人名称',
  `update_time` datetime(3) DEFAULT NULL COMMENT '修改时间',
  `is_del` tinyint(1) DEFAULT '0' COMMENT '是否删除 1:已删除 0:未删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_payment_no` (`payment_no`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_payment_status` (`payment_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀支付表';

-- 支付日志表
CREATE TABLE `seckill_payment_log` (
  `id` bigint NOT NULL COMMENT 'ID',
  `payment_id` bigint NOT NULL COMMENT '支付ID',
  `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
  `operation_type` tinyint NOT NULL COMMENT '操作类型 1:创建支付 2:支付成功 3:支付失败 4:退款',
  `operation_desc` varchar(200) NOT NULL COMMENT '操作描述',
  `request_data` text COMMENT '请求数据',
  `response_data` text COMMENT '响应数据',
  `create_time` datetime(3) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_payment_id` (`payment_id`) USING BTREE,
  KEY `idx_payment_no` (`payment_no`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付日志表';