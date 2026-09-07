package cn.net.susan.seckill.business.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * ES连接预热配置
 * 应用启动时预热ES连接，避免首次访问慢的问题
 *
 * @author 一只朱
 * @date 2026-09-07 15:13
 *
 * "Run the code. Run the world."
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class EsWarmupConfig implements ApplicationRunner {

    private final RestHighLevelClient restHighLevelClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("开始预热ES连接...");
            
            // 执行一个简单的集群健康检查来预热连接
            ClusterHealthRequest request = new ClusterHealthRequest();
            request.timeout("10s");
            request.waitForYellowStatus();
            
            ClusterHealthResponse response = restHighLevelClient.cluster().health(request, RequestOptions.DEFAULT);
            
            log.info("ES连接预热完成，集群状态: {}, 节点数: {}", 
                response.getStatus(), response.getNumberOfNodes());
                
        } catch (Exception e) {
            log.warn("ES连接预热失败，但不影响应用启动: {}", e.getMessage());
        }
    }
}