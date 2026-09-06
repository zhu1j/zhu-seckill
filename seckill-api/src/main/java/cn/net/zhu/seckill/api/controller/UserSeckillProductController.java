package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.entity.response.SeckillResultResponse;
import cn.net.zhu.seckill.business.entity.seckill.UserSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.UserSeckillProductQueryEntity;
import cn.net.zhu.seckill.business.service.UserSeckillProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 秒杀接口控制器
 * 提供秒杀提交、查询秒杀结果两个API；
 * 需要登录鉴权，用户信息从ThreadLocal上下文UserContext获取；
 * 采用异步秒杀模型：提交秒杀不返回最终结果，前端循环轮询getResult接口获取秒杀状态。
 *
 * @author 一只朱
 * @date 2026-09-06 10:53
 *
 * "Run the code. Run the world."
 */

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserSeckillProductController {

    /**
     * 用户秒杀业务服务，封装前置校验、Redis预扣库存、RocketMQ消息投递逻辑
     */
    private final UserSeckillProductService userSeckillProductService;

    /**
     * 提交秒杀请求
     * 接收前端排队页面传过来的商品ID、验证码等参数；执行前置校验、Redis预扣库存、发送MQ消息；
     * 接口void返回，仅代表请求提交成功，**不代表秒杀成功**；
     * 秒杀最终结果需要调用 /getResult 轮询获取
     * @param entity 秒杀请求实体，@Valid执行参数校验
     */
    @PostMapping("/doSeckillProduct")
    public void doSeckillProduct(@Valid @RequestBody UserSeckillProductEntity entity) {
        userSeckillProductService.doSeckillProduct(entity);
    }

    /**
     * 查询秒杀进度与结果，前端轮询调用
     * 返回排队中、处理中、秒杀成功、秒杀失败状态；成功时会携带订单信息
     * @param queryEntity 查询条件：商品id、用户相关查询参数
     * @return SeckillResultResponse 秒杀结果响应对象，包含状态码、进度、提示、订单数据
     */
    @GetMapping("/getResult")
    public SeckillResultResponse getResult(UserSeckillProductQueryEntity queryEntity) {
        return userSeckillProductService.getResultWithProgress(queryEntity);
    }
}

/*
====================业务总结====================
1、模块职责：秒杀对外API控制器，对外暴露异步秒杀能力；本身不实现业务逻辑，只做参数接收与转发；
2、调用链路：前端排队页面AJAX请求 → UserSeckillProductController → UserSeckillProductService；
3、核心流程：
   ① /doSeckillProduct：POST提交秒杀参数，参数校验后交给service；接口返回void，只代表请求入队；
   ② /getResult：GET接口，前端循环轮询；读取Redis中的秒杀进度状态，返回给前端渲染状态；
4、技术设计亮点：
   - 异步模式：提交和结果查询拆分为两个接口，实现削峰；
   - 参数校验：@Valid对RequestBody实体做入参校验；
   - 用户身份依靠UserContext（ThreadLocal），不在接口传参携带用户ID，防止伪造用户；
5、风险点 & 潜在坑：
   - doSeckillProduct无返回值，业务异常会抛出BusinessException，由全局异常处理器捕获返回错误JSON；
   - 前端需要控制轮询间隔，不能高频刷/getResult，压垮Redis；
   - 用户未登录会在service层抛出403异常，本接口不做登录判断，登录校验下沉到Service；
6、模块关联：对接UserSeckillProductService；配合排队页面queue视图使用；依赖全局异常处理器处理BusinessException；
7、生产注意事项：需要对 /doSeckillProduct 接口配置限流防护；前端轮询建议设置最大轮询次数，避免死循环。
*/

