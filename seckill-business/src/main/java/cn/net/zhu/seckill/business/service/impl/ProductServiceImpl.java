package cn.net.zhu.seckill.business.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.net.zhu.seckill.business.entity.ResponsePageEntity;
import cn.net.zhu.seckill.business.entity.product.EsSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.EsSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillProductDetailPageEntity;
import cn.net.zhu.seckill.business.exception.BusinessException;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.util.JsonUtil;
import cn.net.zhu.seckill.business.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.net.zhu.seckill.business.constant.KeyConstant.SECKILL_PRODUCT_DETAIL_PFREFIX;
import static cn.net.zhu.seckill.business.util.BusinessKeyUtil.getProductStockKey;


/**
 * 秒杀商品服务实现类
 * (商品查询全部走ElasticSearch，叠加Redis二级缓存，扛秒杀高并发读流量；
 *  ES负责商品检索、列表、详情展示；真实库存以MySQL/Redis为准，ES仅用于页面展示。)
 *
 * @author 一只朱
 * @date 2026-09-05 18:15
 *
 * "Run the code. Run the world."
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final RestHighLevelClient restHighLevelClient;
    private final RedisUtil redisUtil;

    @Value("${seckill.api.seckillProductEsIndex:seckill-product-es-index-v1}")
    private String seckillProductEsIndex;


    /**
     * 搜索秒杀商品
     *
     * @param seckillProductConditionEntity 查询条件
     * @return 秒杀商品
     */
    public ResponsePageEntity<EsSeckillProductEntity> searchProductList(EsSeckillProductConditionEntity seckillProductConditionEntity) {
        try {
            // 打印接收到的查询条件
            log.info("searchProductList接收到的查询条件: {}", JSON.toJSONString(seckillProductConditionEntity));

            // 构建缓存key
            String cacheKey = buildSearchCacheKey(seckillProductConditionEntity);

            // 先从缓存中获取
            String cachedResult = redisUtil.get(cacheKey);
            if (StringUtils.hasText(cachedResult)) {
                log.info("从缓存中获取搜索结果，key: {}", cacheKey);
                return JSON.parseObject(cachedResult, ResponsePageEntity.class);
            }

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.from(seckillProductConditionEntity.getPageBegin());
            searchSourceBuilder.size(seckillProductConditionEntity.getPageSize());

            // 构建查询条件
            org.elasticsearch.index.query.BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // 商品名称模糊查询
            if (StringUtils.hasLength(seckillProductConditionEntity.getName())) {
                boolQuery.must(QueryBuilders.matchQuery("name", seckillProductConditionEntity.getName()));
            }

            // 秒杀状态查询
            if (seckillProductConditionEntity.getSeckillStatus() != null) {
                Date now = new Date();
                switch (seckillProductConditionEntity.getSeckillStatus()) {
                    case 0: // 未开始
                        boolQuery.must(QueryBuilders.rangeQuery("startTime").gt(now.getTime()));
                        break;
                    case 1: // 进行中
                        boolQuery.must(QueryBuilders.rangeQuery("startTime").lte(now.getTime()));
                        boolQuery.must(QueryBuilders.rangeQuery("endTime").gte(now.getTime()));
                        break;
                    case 2: // 已结束
                        boolQuery.must(QueryBuilders.rangeQuery("endTime").lt(now.getTime()));
                        break;
                }
            }

            // 时间范围查询 - 查询与指定时间范围有重叠的商品
            if (seckillProductConditionEntity.getQueryStartTime() != null) {
                // 商品结束时间 >= 查询开始时间（商品在查询开始时间之后还有效）
                boolQuery.must(QueryBuilders.rangeQuery("endTime").gte(seckillProductConditionEntity.getQueryStartTime().getTime()));
            }
            if (seckillProductConditionEntity.getQueryEndTime() != null) {
                // 商品开始时间 <= 查询结束时间（商品在查询结束时间之前就开始了）
                boolQuery.must(QueryBuilders.rangeQuery("startTime").lte(seckillProductConditionEntity.getQueryEndTime().getTime()));
            }

            searchSourceBuilder.query(boolQuery);

            log.info("searchFromES请求参数: {}", searchSourceBuilder);
            ResponsePageEntity responsePageEntity = ResponsePageEntity.buildEmpty(seckillProductConditionEntity);
            List<EsSeckillProductEntity> productEntities = search(seckillProductEsIndex, searchSourceBuilder,
                    EsSeckillProductEntity.class, responsePageEntity);
            if (CollectionUtils.isEmpty(productEntities)) {
                return ResponsePageEntity.buildEmpty(seckillProductConditionEntity);
            }

            ResponsePageEntity<EsSeckillProductEntity> result = ResponsePageEntity.build(seckillProductConditionEntity, responsePageEntity.getTotalCount(), productEntities);

            // 将结果缓存5分钟
            redisUtil.set(cacheKey, JSON.toJSONString(result), 300);
            log.info("搜索结果已缓存，key: {}", cacheKey);

            return result;
        } catch (IOException e) {
            log.error("从ES中查询商品失败，原因：", e);
            return ResponsePageEntity.buildEmpty(seckillProductConditionEntity);
        }
    }

    /**
     * 构建搜索缓存key
     */
    private String buildSearchCacheKey(EsSeckillProductConditionEntity condition) {
        StringBuilder keyBuilder = new StringBuilder("search:product:");
        keyBuilder.append("page:").append(condition.getPageBegin()).append(":");
        keyBuilder.append("size:").append(condition.getPageSize()).append(":");
        if (StringUtils.hasText(condition.getName())) {
            keyBuilder.append("name:").append(condition.getName()).append(":");
        } else {
            keyBuilder.append("name:all:");
        }
        if (condition.getSeckillStatus() != null) {
            keyBuilder.append("status:").append(condition.getSeckillStatus()).append(":");
        } else {
            keyBuilder.append("status:all:");
        }
        if (condition.getQueryStartTime() != null) {
            keyBuilder.append("startTime:").append(condition.getQueryStartTime().getTime()).append(":");
        } else {
            keyBuilder.append("startTime:all:");
        }
        if (condition.getQueryEndTime() != null) {
            keyBuilder.append("endTime:").append(condition.getQueryEndTime().getTime());
        } else {
            keyBuilder.append("endTime:all");
        }
        return keyBuilder.toString();
    }

    /**
     * 获取商品信息
     *
     * @param id 秒杀商品ID
     * @return 商品信息
     */
    public SeckillProductDetailPageEntity getProductInfo(Long id) {
        String json = redisUtil.get(getKey(id.toString()));
        if (json == null) {
            return null;
        }
        return JsonUtil.parseRedisEntity(json, SeckillProductDetailPageEntity.class);
    }

    /**
     * 获取秒杀商品详情
     *
     * @param id 秒杀商品ID
     * @return 商品详情
     */
    public SeckillProductDetailPageEntity getProductDetail(Long id) {
        SeckillProductDetailPageEntity seckillProductDetailEntity = getProductInfo(id);
        if (seckillProductDetailEntity == null) {
            return null;
        }
        String stockValue = redisUtil.get(getProductStockKey(id));
        seckillProductDetailEntity.setWithHoldQuantity(Integer.parseInt(stockValue));
        calcSecondAndStatus(seckillProductDetailEntity);
        return seckillProductDetailEntity;
    }

    private void calcSecondAndStatus(SeckillProductDetailPageEntity seckillProductDetailEntity) {
        Date startTime = seckillProductDetailEntity.getStartTime();
        Date endTime = seckillProductDetailEntity.getEndTime();
        Date nowTime = new Date();

        //如果开始时间大于当前时间，说明秒杀还未开始
        if (startTime.after(nowTime)) {
            long betweenSecond = DateUtil.between(startTime, nowTime, DateUnit.SECOND);
            //秒杀前60秒开始倒计时
            if (betweenSecond <= 60) {
                seckillProductDetailEntity.setSeckillStatus(1);
                seckillProductDetailEntity.setRemainSeconds((int) betweenSecond);
            } else {
                seckillProductDetailEntity.setSeckillStatus(0);
            }
        } else {
            if (endTime.after(nowTime)) {
                seckillProductDetailEntity.setSeckillStatus(2);
            } else {
                seckillProductDetailEntity.setSeckillStatus(3);
            }
        }
    }

    private String getKey(String id) {
        return String.format("%s%s", SECKILL_PRODUCT_DETAIL_PFREFIX, id);
    }

    /**
     * 查询数据
     *
     * @param idxName            index
     * @param builder            查询参数
     * @param aClass             结果类对象
     * @param responsePageEntity 总记录数
     * @return java.util.List<T>
     */
    private <T> List<T> search(String idxName, SearchSourceBuilder builder, Class<T> aClass, ResponsePageEntity responsePageEntity) throws IOException {
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        int total = (int) response.getHits().getTotalHits().value;
        responsePageEntity.setTotalCount(total);
        return Arrays.stream(hits).map(hit -> JSON.parseObject(hit.getSourceAsString(), aClass)).collect(Collectors.toList());
    }

    /**
     * 修改ES中商品库存
     *
     * @param id    秒杀商品ID
     * @param stock 库存
     * @throws IOException 异常
     */
    public void updateEsProductStock(Long id, Integer stock) {
        try {
            //构建修改请求
            UpdateRequest request = new UpdateRequest(seckillProductEsIndex, id.toString());
            request.doc("withHoldQuantity", stock);
            restHighLevelClient.update(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.info("updateEsProductStock失败：", e);
            throw new BusinessException("updateEsProductStock失败");
        }
    }

    /**
     * 从ES中根据ID获取秒杀商品信息
     *
     * @param id 秒杀商品ID
     * @return ES秒杀商品实体
     */
    public EsSeckillProductEntity getProductFromES(Long id) {
        try {
            GetRequest getRequest = new GetRequest(seckillProductEsIndex, id.toString());
            GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

            if (getResponse.isExists()) {
                String sourceAsString = getResponse.getSourceAsString();
                return JSON.parseObject(sourceAsString, EsSeckillProductEntity.class);
            } else {
                log.warn("从ES中未找到ID为{}的商品信息", id);
                return null;
            }
        } catch (IOException e) {
            log.error("从ES中获取商品信息失败，商品ID：{}，原因：", id, e);
            throw new BusinessException("从ES中获取商品信息失败");
        }
    }
}

