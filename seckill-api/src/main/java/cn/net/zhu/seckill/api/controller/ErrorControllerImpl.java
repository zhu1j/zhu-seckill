package cn.net.zhu.seckill.api.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 处理错误页面
 *
 * @author 一只朱
 * @date 2026-09-07 16:28
 *
 * "Run the code. Run the world."
 */

@Controller
public class ErrorControllerImpl implements ErrorController {

    private static final String ERROR_PARAM = "javax.servlet.error.exception";

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request) throws Throwable {
        if (request.getAttribute(ERROR_PARAM) != null) {
            throw (Throwable) request.getAttribute("javax.servlet.error.exception");
        }
    }
}
