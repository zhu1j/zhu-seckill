package cn.net.zhu.seckill.business.util;

import cn.hutool.json.JSONUtil;
import cn.net.zhu.seckill.business.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * 发起http请求工具类
 *
 * @author 一只朱
 * @date 2026-09-07 16:03
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Component
public class HttpUtil {

    @Autowired
    private RestTemplate restTemplate;


    /**
     * 发送post请求
     *
     * @param requestUrl    请求地址
     * @param requestParam  参数
     * @param typeReference 返回实体类型
     * @param <T>           泛型
     * @return 返回值
     */
    public <T> T doPost(String requestUrl, Object requestParam, ParameterizedTypeReference<ApiResult<T>> typeReference) {
        HttpEntity httpEntity = new HttpEntity(requestParam);
        log.info("请求接口地址:{}，请求参数:{}", requestUrl, JSONUtil.toJsonStr(requestParam));
        ResponseEntity<ApiResult<T>> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.POST, httpEntity, typeReference);
        if (Objects.nonNull(responseEntity) && responseEntity.getStatusCode() == HttpStatus.OK) {
            ApiResult<T> body = responseEntity.getBody();
            log.info("请求接口地址:{}，返回数据:{}", requestUrl, JSONUtil.toJsonStr(body));
            if (body.getCode() == ApiResult.OK) {
                return body.getData();
            } else {
                throw new BusinessException(body.getMessage());
            }

        }
        throw new BusinessException("请求接口失败");
    }

}
