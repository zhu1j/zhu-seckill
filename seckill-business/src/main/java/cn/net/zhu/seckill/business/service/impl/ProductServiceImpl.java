package cn.net.zhu.seckill.business.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.net.zhu.seckill.business.entity.ResponsePageEntity;
import cn.net.zhu.seckill.business.entity.product.ESSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.ESSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillProductDetailPageEntity;
import cn.net.zhu.seckill.business.service.ProductService;
import cn.net.zhu.seckill.business.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /**
     * ElasticSearch高级客户端，操作ES索引
     */
    private final RestHighLevelClient restHighLevelClient;

    /**
     * Redis工具，实现商品列表、详情二级缓存
     */
    private final RedisUtil redisUtil;

    /**
     * ES索引名称：秒杀商品索引
     */
    private static final String ES_INDEX = "seckill-product-es-index-v1";

    /**
     * 商品详情Redis缓存key前缀 seckillProductDetail:{商品id}
     */
    private static final String DETAIL_CACHE_PREFIX = "seckillProductDetail:";

    /**
     * 商品搜索列表缓存key前缀 productSearch:{条件json}
     */
    private static final String SEARCH_CACHE_PREFIX = "productSearch:";

    /**
     * 分页搜索秒杀商品列表
     * 先查Redis缓存，缓存未命中再查询ES，查询结果回写Redis，缓存5分钟
     * @param condition 查询条件实体：名称、分页参数
     * @return 分页商品结果集
     */
    @Override
    public ResponsePageEntity<ESSeckillProductEntity> searchProductList(
            ESSeckillProductConditionEntity condition) {
        try {
            // 1.构建搜索缓存key，优先查询Redis缓存
            String cacheKey = buildSearchCacheKey(condition);
            String cachedResult = redisUtil.get(cacheKey);
            if (StringUtils.hasText(cachedResult)) {
                return JSON.parseObject(cachedResult, ResponsePageEntity.class);
            }

            // 2.组装ES查询条件：分页、名称模糊匹配、只查询正在进行的秒杀商品
            SearchSourceBuilder builder = new SearchSourceBuilder();
            builder.from(condition.getPageBegin());
            builder.size(condition.getPageSize());

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            // 商品名称匹配条件
            if (StringUtils.hasLength(condition.getName())) {
                boolQuery.must(QueryBuilders.matchQuery("name", condition.getName()));
            }
            // 时间条件：开始时间<=当前时间，结束时间>=当前时间，只查进行中的秒杀
            Date now = new Date();
            boolQuery.must(QueryBuilders.rangeQuery("startTime").lte(now.getTime()));
            boolQuery.must(QueryBuilders.rangeQuery("endTime").gte(now.getTime()));
            builder.query(boolQuery);

            // 3.执行ES搜索请求
            SearchRequest request = new SearchRequest(ES_INDEX);
            request.source(builder);
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            // 4.解析ES返回结果，封装分页对象
            List<ESSeckillProductEntity> list = Arrays.stream(response.getHits().getHits())
                    .map(hit -> JSON.parseObject(hit.getSourceAsString(), ESSeckillProductEntity.class))
                    .collect(Collectors.toList());
            ResponsePageEntity<ESSeckillProductEntity> result = new ResponsePageEntity<>();
            result.setData(list);
            result.setTotalCount(response.getHits().getTotalHits().value);
            result.setPageNo(condition.getPageNo());
            result.setPageSize(condition.getPageSize());

            // 5.查询结果写入Redis缓存，有效期5分钟
            redisUtil.set(cacheKey, JSON.toJSONString(result), 300);
            return result;
        } catch (Exception e) {
            log.error("搜索商品列表失败", e);
            // 异常返回空分页，避免雪崩
            return ResponsePageEntity.buildEmpty();
        }
    }

    /**
     * 获取商品详情页面数据，Redis二级缓存
     * @param id 商品id
     * @return 商品详情页VO
     */
    @Override
    public SeckillProductDetailPageEntity getProductDetail(Long id) {
        // 1.优先读取Redis详情缓存
        String cacheKey = DETAIL_CACHE_PREFIX + id;
        String cached = redisUtil.get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return JSON.parseObject(cached, SeckillProductDetailPageEntity.class);
        }
        // 2.缓存未命中，查询ES获取商品详情
        SeckillProductDetailPageEntity detail = getProductInfo(id);
        // 3.查询不为空，回写Redis缓存5分钟
        if (detail != null) {
            redisUtil.set(cacheKey, JSON.toJSONString(detail), 300);
        }
        return detail;
    }

    /**
     * 根据商品id从ES查询原始商品数据，组装页面详情VO，计算秒杀状态
     * @param id 商品id
     * @return 商品详情VO，查询不到返回null
     */
    @Override
    public SeckillProductDetailPageEntity getProductInfo(Long id) {
        try {
            GetRequest request = new GetRequest(ES_INDEX, id.toString());
            GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            if (response.isExists()) {
                ESSeckillProductEntity esProduct = JSON.parseObject(
                        response.getSourceAsString(), ESSeckillProductEntity.class);
                SeckillProductDetailPageEntity detail = new SeckillProductDetailPageEntity();
                // 属性拷贝，正式项目建议替换BeanUtils.copyProperties
                detail.setId(esProduct.getId());
                detail.setName(esProduct.getName());
                detail.setPrice(esProduct.getPrice());
                detail.setStartTime(esProduct.getStartTime());
                detail.setEndTime(esProduct.getEndTime());
                detail.setWithHoldQuantity(esProduct.getWithHoldQuantity());
                detail.setRemainQuantity(esProduct.getRemainQuantity());
                detail.setCover(esProduct.getCover());
                // 根据当前时间计算秒杀状态、剩余倒计时秒数
                calcSecondAndStatus(detail);
                return detail;
            }
        } catch (Exception e) {
            log.error("查询商品详情失败, id={}", id, e);
        }
        return null;
    }

    /**
     * 计算秒杀状态和剩余倒计时秒数
     * status:0未开始；2进行中；3已结束
     * @param detail 商品详情VO
     */
    private void calcSecondAndStatus(SeckillProductDetailPageEntity detail) {
        Date now = new Date();
        Date startTime = detail.getStartTime();
        Date endTime = detail.getEndTime();
        if (startTime.after(now)) {
            // 秒杀未开始：设置状态0，距离开始剩余秒数
            detail.setSeckillStatus(0);
            detail.setRemainSeconds(DateUtil.between(startTime, now, DateUnit.SECOND));
        } else if (endTime.after(now)) {
            // 秒杀进行中：状态2，倒计时置0
            detail.setSeckillStatus(2);
            detail.setRemainSeconds(0L);
        } else {
            // 秒杀已结束：状态3，倒计时置0
            detail.setSeckillStatus(3);
            detail.setRemainSeconds(0L);
        }
    }

    /**
     * 更新ES中商品展示库存（仅页面展示使用，不做真实扣减）
     * @param id 商品id
     * @param stock 需要更新的展示库存
     */
    @Override
    public void updateEsProductStock(Long id, Integer stock) {
        try {
            UpdateRequest request = new UpdateRequest(ES_INDEX, id.toString());
            request.doc("withHoldQuantity", stock);
            restHighLevelClient.update(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("更新ES库存失败, id={}, stock={}", id, stock, e);
        }
    }

    /**
     * 直接从ES获取原始商品实体对象
     * @param id 商品id
     * @return ES原始实体，不存在返回null
     */
    @Override
    public ESSeckillProductEntity getProductFromES(Long id) {
        try {
            GetRequest request = new GetRequest(ES_INDEX, id.toString());
            GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            if (response.isExists()) {
                return JSON.parseObject(response.getSourceAsString(), ESSeckillProductEntity.class);
            }
        } catch (Exception e) {
            log.error("从ES查询商品失败, id={}", id, e);
        }
        return null;
    }

    /**
     * 构建商品搜索列表缓存key，把查询条件序列化为JSON拼接key
     * @param condition 查询条件
     * @return redis缓存key
     */
    private String buildSearchCacheKey(ESSeckillProductConditionEntity condition) {
        return SEARCH_CACHE_PREFIX + JSON.toJSONString(condition);
    }
}

/*
====================业务总结====================
1、秒杀商品读层实现，架构：Redis缓存 → ElasticSearch；MySQL只负责写，扛住秒杀大量商品列表、详情读请求；
2、searchProductList：商品列表查询，条件分页，默认只查【正在进行】的秒杀；查询结果缓存Redis5分钟；ES/Redis异常返回空分页，防止服务雪崩；
3、getProductDetail：商品详情接口，先读Redis缓存，缓存失效访问ES，结果回写缓存；
4、getProductInfo：直接访问ES拿原始数据，组装VO，调用calcSecondAndStatus计算秒杀状态（未开始/进行中/已结束）和倒计时；
5、updateEsProductStock：更新ES展示库存；⚠️ES库存仅前端页面展示，真实扣减库存以Redis+MySQL为准；ES更新失败只打日志，不阻断主流程；
6、getProductFromES：内部工具方法，直接获取ES原始实体；
7、缓存设计：
   - 列表缓存key由查询条件JSON生成；不同分页、不同查询条件生成不同key；
   - 列表、详情缓存过期时间固定5分钟；
8、风险点：
   - 缓存key使用JSON序列化条件，条件字段顺序变化会造成key不一致；
   - ES库存和真实库存存在短暂不一致，属于最终一致性；
   - ES宕机降级返回空列表，前端需要处理空数据展示；
9、调用链路：前端商品列表页 → searchProductList（Redis→ES）；商品详情页 → getProductDetail（Redis→ES）。
*/

