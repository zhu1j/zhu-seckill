package cn.net.zhu.seckill.business.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ配置类
 *
 * @author 一只朱
 * @date 2026-09-07 15:15
 *
 * "Run the code. Run the world."
 */

@Configuration
@Slf4j
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-message-timeout}")
    private int sendMessageTimeout;

    /**
     * 配置 RocketMQ 生产者
     */
    @Bean
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setProducerGroup(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(2);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.setMaxMessageSize(4 * 1024 * 1024); // 4MB

        // 禁用VIP通道，避免端口映射问题
        producer.setVipChannelEnabled(false);
        // 设置实例名称，避免冲突
        producer.setInstanceName("producer_" + System.currentTimeMillis());
        // 设置单元化名称
        producer.setUnitName("zhu-seckill-unit");

        log.info("RocketMQ Producer配置完成，name-server: {}, producer-group: {}, send-message-timeout: {}ms",
                nameServer, producerGroup, sendMessageTimeout);

        return producer;
    }

    /**
     * 初始化 RocketMQTemplate
     */
    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(defaultMQProducer());

        log.info("RocketMQTemplate 初始化完成");
        return template;
    }
}