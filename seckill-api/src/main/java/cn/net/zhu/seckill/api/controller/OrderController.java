package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.api.dto.CancelOrderRequest;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillOrderTradeEntity;
import cn.net.zhu.seckill.business.service.SeckillOrderTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

/**
 * 订单页面控制器
 * 负责订单相关页面跳转：支付页、订单详情页、订单列表页；同时提供取消订单AJAX接口；
 * 采用服务端模板渲染，返回thymeleaf视图；取消订单接口返回JSON；
 * 查询全部调用SeckillOrderTradeService，数据库使用业务orderCode作为对外标识，不暴露数据库主键id。
 *
 * @author 一只朱
 * @date 2026-09-06 11:03
 *
 * "Run the code. Run the world."
 */

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderController {

    /**
     * 秒杀订单业务服务，封装订单查询、取消订单逻辑
     */
    private final SeckillOrderTradeService seckillOrderTradeService;

    /**
     * 跳转到订单支付页面
     * @param orderCode 业务订单编码
     * @param model 模板数据模型
     * @return 视图名称 payment，渲染支付页面
     */
    @GetMapping("/payment")
    public String goToPayment(@RequestParam("orderCode") String orderCode, Model model) {
        SeckillOrderTradeEntity order = seckillOrderTradeService.getOrderDetailByOrderNo(orderCode);
        model.addAttribute("orderDetail", order);
        return "payment";
    }

    /**
     * 跳转到订单详情页面
     * @param orderCode 业务订单编码
     * @param model 模板数据模型
     * @return 视图名称 order_detail，渲染订单详情页面
     */
    @GetMapping("/order/detail")
    public String goToOrderDetail(@RequestParam("orderCode") String orderCode, Model model) {
        SeckillOrderTradeEntity order = seckillOrderTradeService.getOrderDetailByOrderNo(orderCode);
        model.addAttribute("orderDetail", order);
        return "order_detail";
    }

    /**
     * 跳转到订单列表页面，支持多条件过滤分页查询
     * @param userId 用户ID，可选过滤条件
     * @param orderNo 业务订单号，可选过滤条件
     * @param productName 商品名称模糊查询，可选
     * @param status 订单状态，可选过滤
     * @param startTime 下单开始时间，时间格式 yyyy‑MM‑dd HH:mm:ss
     * @param endTime 下单结束时间
     * @param pageNo 当前页码，默认1
     * @param pageSize 每页条数，默认10
     * @param model 模板模型，携带列表、总条数、分页参数传给页面
     * @return 视图名称 order_list，渲染订单列表页面
     */
    @GetMapping("/order/list")
    public String goToOrderList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Model model) {
        // 组装查询条件实体
        SeckillOrderTradeConditionEntity condition = new SeckillOrderTradeConditionEntity();
        condition.setUserId(userId);
        condition.setCode(orderNo);
        condition.setProductName(productName);
        condition.setOrderStatus(status);
        condition.setOrderTimeStart(startTime);
        condition.setOrderTimeEnd(endTime);
        condition.setPageNo(pageNo);
        condition.setPageSize(pageSize);

        // 查询分页数据与总记录数
        List<SeckillOrderTradeEntity> orderList = seckillOrderTradeService.getOrderList(condition);
        int totalCount = seckillOrderTradeService.getOrderCount(condition);

        // 将数据放入模板，供Thymeleaf渲染分页
        model.addAttribute("orderList", orderList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageNo", pageNo);
        model.addAttribute("pageSize", pageSize);
        return "order_list";
    }

    /**
     * AJAX取消订单接口
     * @param request 取消订单请求DTO，携带orderCode，@Valid做参数校验
     * @return Boolean true取消成功；失败抛出RuntimeException，由全局异常处理器捕获
     */
    @PostMapping("/order/cancel")
    @ResponseBody
    public Boolean cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
        boolean result = seckillOrderTradeService.cancelOrder(request.getOrderCode());
        if (!result) {
            throw new RuntimeException("订单取消失败");
        }
        return true;
    }
}

/*
====================业务总结====================
1、模块职责：订单页面控制器；提供支付页、订单详情、订单列表页面跳转；对外提供取消订单AJAX接口；页面使用服务端Thymeleaf模板渲染。
2、调用链路：
浏览器请求 → OrderController → SeckillOrderTradeService → SeckillOrderTradeMapper；
前端列表页面AJAX POST调用 /order/cancel 发起取消订单。
3、核心流程：
① /payment：传入orderCode查询订单，渲染支付页面；
② /order/detail：传入orderCode查询订单，渲染订单详情；
③ /order/list：多条件+时间范围+分页参数组装查询条件；查询列表与总条数传给模板渲染分页；
④ /order/cancel：JSON接口调用service取消订单；service返回false则抛出运行时异常，交给全局异常处理器返回错误提示。
4、技术设计亮点：
- 视图与AJAX接口混合：页面跳转返回视图；取消订单添加@ResponseBody返回JSON；
- @DateTimeFormat 完成前端时间字符串自动转为Date对象；
- 统一使用业务orderCode操作订单，不暴露数据库主键ID；
- 分页参数设置默认值 pageNo=1、pageSize=10，避免不传参数报错。
5、风险点 & 潜在坑：
- 没有做用户权限校验：任何人传入orderCode都可以查询订单；需要增加登录校验，校验订单归属用户；
- cancelOrder只调用service层取消订单状态，**没有触发库存回滚逻辑**；service层本身不处理库存恢复，存在库存不一致风险；
- service返回false直接抛RuntimeException，属于非业务异常；需要替换为BusinessException统一异常体系；
- orderDetail为null时Thymeleaf模板未做空判断，页面会报空指针；
- 时间范围查询如果前端传大量时间区间，容易产生慢SQL，需要限制查询跨度。
6、模块关联：依赖SeckillOrderTradeService；对应模板文件 payment.html、order_detail.html、order_list.html；依赖全局异常处理器捕获RuntimeException。
7、生产注意事项：
- 必须增加用户鉴权：只能查询、取消自己名下的订单，防止越权；
- 调用cancelOrder成功后，上层需要补充库存回滚逻辑（DB锁定库存+Redis库存恢复）；
- order/list接口要做参数防护，禁止超大pageSize，防止一次性查询海量订单拖垮数据库。
*/

