package cn.net.zhu.seckill.api.exception;

import cn.net.zhu.seckill.business.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 *  全局异常处理器类
 *
 * @author 一只朱
 * @date 2026-08-23 03:06
 *
 * "Run the code. Run the world."
 */

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e){
        Map<String,Object> result = new HashMap<>();
        result.put("success",false);
        result.put("code",e.getCode());
        result.put("message",e.getMessage());
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleException(Exception e) {
        Map<String,Object> result = new HashMap<>();
        result.put("success",false);
        result.put("code",500);
        result.put("message","系统内部错误，请稍后重试");
        return ResponseEntity.ok(result);
    }
}
