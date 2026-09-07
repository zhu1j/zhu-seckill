package cn.net.zhu.seckill.api.controller;

import cn.net.zhu.seckill.business.entity.ResponsePageEntity;
import cn.net.zhu.seckill.business.entity.product.EsSeckillProductConditionEntity;
import cn.net.zhu.seckill.business.entity.seckill.EsSeckillProductEntity;
import cn.net.zhu.seckill.business.entity.seckill.SeckillProductDetailPageEntity;
import cn.net.zhu.seckill.business.service.ProductService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * 商品控制器
 * (秒杀商品页面跳转与商品查询接口；
 * 包含商品列表页、商品详情页、排队中转页面；读请求全部交由ProductService，底层走Redis+ES；
 * 页面跳转接口不需要鉴权；部分接口供前端页面AJAX调用。)
 *
 * @author 一只朱
 * @date 2026-09-06 10:32
 *
 * "Run the code. Run the world."
 */

@Api(tags = "商品操作")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class ProductController {

    /**
     * 商品服务，封装Redis缓存、ES查询逻辑
     */
    private final ProductService productService;

    /**
     * 跳转到商品列表页面
     * @return ModelAndView 返回product_list视图，渲染秒杀商品列表页面
     */
    @GetMapping({"/", "/product/list"})
    public ModelAndView goToProductList() {
        return new ModelAndView("product_list");
    }

    /**
     * 商品列表搜索接口，前端AJAX异步调用
     * @param condition 搜索条件实体：关键词、分页参数
     * @return ResponsePageEntity<ESSeckillProductEntity> ES分页商品数据
     */
    @PostMapping("/searchProductList")
    public ResponsePageEntity<EsSeckillProductEntity> searchProductList(
            @RequestBody EsSeckillProductConditionEntity condition) {
        return productService.searchProductList(condition);
    }

    /**
     * 跳转到商品详情页面
     * @param id 商品id，允许不传
     * @return ModelAndView 商品详情视图；id为空重定向到商品列表页；携带productDetail数据供模板渲染
     */
    @GetMapping("/product/detail")
    public ModelAndView goToProductDetail(@RequestParam(value = "id", required = false) Long id) {
        ModelAndView modelAndView = new ModelAndView("product_detail");
        // id为空，重定向到商品列表页，防止访问空白详情页
        if (id == null) {
            modelAndView.setViewName("redirect:/product/list");
            return modelAndView;
        }
        // 查询商品详情（优先Redis缓存，缓存未命中查询ES）
        SeckillProductDetailPageEntity detail = productService.getProductDetail(id);
        // 将商品详情放入模板变量，供前端Thymeleaf页面渲染
        modelAndView.addObject("productDetail", detail);
        return modelAndView;
    }

    /**
     * 跳转到秒杀排队中转页面
     * @param seckillProductId 秒杀商品ID
     * @param uuid 图形验证码uuid
     * @param code 用户输入的图形验证码
     * @return ModelAndView 返回queue排队页面，把参数传给排队页面模板
     */
    @GetMapping("/queue")
    public ModelAndView goToQueue(@RequestParam("seckillProductId") Long seckillProductId,
                                  @RequestParam("uuid") String uuid,
                                  @RequestParam("code") String code) {
        ModelAndView modelAndView = new ModelAndView("queue");
        // 将秒杀商品id、验证码参数传递给排队页面模板，后续排队页面发起真实秒杀请求
        modelAndView.addObject("seckillProductId", seckillProductId);
        modelAndView.addObject("uuid", uuid);
        modelAndView.addObject("code", code);
        return modelAndView;
    }
}

/*
====================业务总结====================
1、模块职责：商品前端控制器，负责页面视图跳转与AJAX商品查询；全部接口为公开接口，无需JWT鉴权；
2、调用链路：前端浏览器请求 → ProductController → ProductService → Redis缓存 → ElasticSearch；
3、核心流程：
   - 访问根路径 / 或者 /product/list 跳转商品列表页面；页面AJAX POST调用 /searchProductList 获取分页商品数据；
   - /product/detail?id=xxx：跳转商品详情；id为空自动重定向列表；查询到的详情对象传给Thymeleaf模板渲染；
   - /queue：秒杀前置排队中转页；接收商品id、验证码uuid、验证码，页面拿到参数后发起秒杀下单请求；
4、技术设计亮点：
   - MVC分离：页面跳转返回ModelAndView视图；查询数据接口返回JSON；
   - 入参容错：商品详情id不传直接重定向，避免空白页面；
   - 读流量全部下沉到ProductService，复用Redis‑ES二级缓存能力；
5、风险点 & 潜在坑：
   - getProductDetail查询返回null时，模板拿到null对象，前端thymeleaf需要做空判断，否则页面报错；
   - /queue接口仅做页面中转，不校验验证码；验证码校验逻辑放在后续秒杀接口；
   - ES库存仅用于页面展示，真实库存以Redis+MySQL为准；
6、模块关联：和ProductServiceImpl配合；页面跳转视图对应resources/templates下的product_list、product_detail、queue；
7、生产注意事项：ES/Redis故障时searchProductList返回空分页，前端需要做空数据友好展示。
*/
