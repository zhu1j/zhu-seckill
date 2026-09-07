package cn.net.zhu.seckill.api.exception;

import cn.net.zhu.seckill.business.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *  全局异常处理器类
 *
 * @author 一只朱
 * @date 2026-08-23 03:06
 *
 * "Run the code. Run the world."
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.error("业务异常：", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", e.getCode());
        result.put("message", e.getMessage());
        return ResponseEntity.ok(result);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("参数校验异常：", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", HttpStatus.BAD_REQUEST.value());

        StringBuilder message = new StringBuilder();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }
        result.put("message", message.toString());
        return ResponseEntity.ok(result);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException e) {
        log.error("绑定异常：", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", HttpStatus.BAD_REQUEST.value());

        StringBuilder message = new StringBuilder();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }
        result.put("message", message.toString());
        return ResponseEntity.ok(result);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("约束违反异常：", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", HttpStatus.BAD_REQUEST.value());

        StringBuilder message = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            message.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
        }
        result.put("message", message.toString());
        return ResponseEntity.ok(result);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常：", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        result.put("message", "系统内部错误，请稍后重试");
        return ResponseEntity.ok(result);
    }
}
