package cn.net.zhu.seckill.business.exception;

import lombok.Getter;

/**
 *  业务异常类（秒杀业务专属的自定义业务异常类）
 *
 * @author 一只朱
 * @date 2026-08-23 15:26
 *
 * "Run the code. Run the world."
 */

@Getter
public class BusinessException extends RuntimeException{
    public static final long serialVersionUID = -6735897190745766939L;

    /**
     * 异常码
     */
    private final int code;
    /**
     * 具体异常信息
     */
    private final String message;

    public BusinessException(int code, String message){
        super(message);
        this.code = code;
        this.message = message;
    }
    public BusinessException(String message) {
        this(1, message);// 👉 调用上面的 (int code,String message)构造
    }
}
